/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.dash.valid.ars;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringConstants;

/**
 * Local, on-disk cache for {@link AntigenRecognitionSiteLoader}'s parsed ARS/G-group map, keyed
 * by hladb version. Fetching and parsing {@code hla_ambigs.xml.zip} costs real time (a real,
 * decompressed 1.5GB+ XML file at the time of writing, streamed and tokenized every time it's
 * needed) -- for a <em>pinned</em> hladb version (e.g. {@code "3.65.0"}) that data can never
 * change once IMGT/HLA publishes it, so there's no reason to re-fetch and re-parse it on every
 * process run.
 * <p>
 * {@link GLStringConstants#LATEST_HLADB} ({@code "Latest"}) is the one exception: it's a moving
 * GitHub branch, not a pinned release, so caching it forever would silently serve stale data
 * after IMGT/HLA's next release. Entries for it expire after {@link #LATEST_TTL_MILLIS} instead
 * of being kept indefinitely.
 * <p>
 * Every method here is purely best-effort: a missing, corrupt, unreadable, or expired cache
 * entry is treated as a plain cache miss (never thrown), and a failure to write is logged and
 * swallowed -- the caller already has a correct, freshly-parsed result regardless of whether the
 * cache write succeeds. Nothing about correctness depends on this class working.
 */
class AntigenRecognitionSiteCache {
	private static final Logger LOGGER = Logger.getLogger(AntigenRecognitionSiteCache.class.getName());

	/**
	 * Overrides the cache directory (default: {@code ~/.dash-cache/ars}). Not something a user
	 * needs to set for normal use -- this exists for edge cases like a read-only home directory
	 * or a container, the same spirit as {@link GLStringConstants#HLADB_PROPERTY} and friends.
	 */
	public static final String ARS_CACHE_DIR_PROPERTY = "org.dash.arsCacheDir";

	private static final String CACHED_AT_PREFIX = "#cachedAt=";
	private static final long LATEST_TTL_MILLIS = TimeUnit.HOURS.toMillis(24);

	private AntigenRecognitionSiteCache() {
	}

	/**
	 * @param hladb the hladb this data would be for (never null -- callers already default it)
	 * @return the cached ARS map, or {@code null} on any cache miss (not present, unreadable,
	 *         wrong format, or -- for {@link GLStringConstants#LATEST_HLADB} only -- expired)
	 */
	static HashMap<String, HashSet<String>> read(String hladb) {
		File file = cacheFile(hladb);

		if (!file.isFile()) {
			return null;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String header = reader.readLine();
			if (header == null || !header.startsWith(CACHED_AT_PREFIX)) {
				return null;
			}

			long cachedAt = Long.parseLong(header.substring(CACHED_AT_PREFIX.length()));
			if (GLStringConstants.LATEST_HLADB.equals(hladb) && System.currentTimeMillis() - cachedAt > LATEST_TTL_MILLIS) {
				return null;
			}

			HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
			String line;
			while ((line = reader.readLine()) != null) {
				int tab = line.indexOf('\t');
				if (tab < 0) {
					continue;
				}

				String arsCode = line.substring(0, tab);
				String allelesJoined = line.substring(tab + 1);
				HashSet<String> alleles = new HashSet<String>();
				// An empty gGroup (no gGroupAllele children) is stored as an empty string here --
				// "".split(",") returns [""], not [], so this must be checked explicitly or a
				// bogus single empty-string allele would be reconstructed.
				if (!allelesJoined.isEmpty()) {
					alleles.addAll(Arrays.asList(allelesJoined.split(",")));
				}
				arsMap.put(arsCode, alleles);
			}

			return arsMap;
		}
		catch (Exception e) {
			LOGGER.info("Ignoring unreadable ARS cache file " + file + ": " + e);
			return null;
		}
	}

	/**
	 * Best-effort write; any failure is logged and swallowed rather than propagated, since a
	 * cache write failing doesn't affect the correctness of the result the caller already has.
	 *
	 * @param hladb the hladb this data is for
	 * @param arsMap the freshly-parsed map to persist
	 */
	static void write(String hladb, HashMap<String, HashSet<String>> arsMap) {
		File dir = cacheDir();
		File tempFile = null;

		try {
			Files.createDirectories(dir.toPath());

			// Created directly inside the target directory (not the system temp dir) so the
			// final Files.move() below is guaranteed to be on the same filesystem -- required
			// for ATOMIC_MOVE to actually be atomic rather than silently falling back to a
			// non-atomic copy-then-delete.
			tempFile = File.createTempFile("ars-", ".tmp", dir);

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
				writer.write(CACHED_AT_PREFIX + System.currentTimeMillis());
				writer.newLine();

				for (Map.Entry<String, HashSet<String>> entry : arsMap.entrySet()) {
					// A null key here would throw (BufferedWriter#write(null) is an NPE) and be
					// swallowed by the catch below -- confirmed as a real failure mode for the
					// analogous CommonWellDocumentedCache (a null map key does occur in real
					// data there). arsCode is always non-null by construction in this loader, but
					// skipping defensively costs nothing and keeps the rest of a real map
					// cacheable even if that ever stops being true.
					if (entry.getKey() == null) {
						continue;
					}
					writer.write(entry.getKey());
					writer.write('\t');
					writer.write(String.join(",", entry.getValue()));
					writer.newLine();
				}
			}

			Files.move(tempFile.toPath(), cacheFile(hladb).toPath(),
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (Exception e) {
			LOGGER.info("Couldn't write ARS cache for hladb " + hladb + ": " + e);
			// Best-effort cleanup so a write failure (whatever the cause) doesn't leave orphaned
			// .tmp files behind indefinitely -- this is the one path Files.move() above never
			// reaches, so tempFile would otherwise just sit in the cache directory forever.
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}

	// Package-private (not private) so tests can locate the exact file this class would read
	// from/write to, to exercise edge cases (corruption, expiry) that require crafting a cache
	// file's content directly rather than going through write().
	static File cacheFile(String hladb) {
		// Matches the same normalization AntigenRecognitionSiteLoader#loadGGroups uses to build
		// the fetch URL, so the cache key exactly tracks whatever actually determines the
		// content (e.g. "3.65.0" and "3.65.0" -- the only form this ever legitimately takes --
		// both normalize to "3650"). Sanitized defensively in case some future hladb value ever
		// contains characters that aren't safe in a filename.
		String normalized = hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING);
		String safeName = normalized.replaceAll("[^A-Za-z0-9_-]", "_");
		return new File(cacheDir(), "ars-" + safeName + ".cache");
	}

	private static File cacheDir() {
		String override = System.getProperty(ARS_CACHE_DIR_PROPERTY);
		if (override != null && !override.isEmpty()) {
			return new File(override);
		}

		return new File(System.getProperty("user.home"), ".dash-cache" + File.separator + "ars");
	}
}
