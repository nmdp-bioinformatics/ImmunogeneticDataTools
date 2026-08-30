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
package org.dash.valid.cwd;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringConstants;

/**
 * Local, on-disk cache for {@link CommonWellDocumentedLoader#loadFromIMGT}'s parsed accession
 * map, keyed by hladb version. Same rationale, same design as
 * {@link org.dash.valid.ars.AntigenRecognitionSiteCache} (see its javadoc for the full
 * background) -- {@code hla.xml.zip} is a real, multi-MB-per-release file that's expensive to
 * fetch and parse, and for a <em>pinned</em> hladb version that data can never change once
 * IMGT/HLA publishes it.
 * <p>
 * {@link GLStringConstants#LATEST_HLADB} entries expire after {@link #LATEST_TTL_MILLIS} instead
 * of being kept indefinitely, since {@code "Latest"} is a moving GitHub branch, not a pinned
 * release.
 * <p>
 * Purely best-effort throughout: a missing, corrupt, unreadable, or expired entry is a plain
 * cache miss (never thrown), and a write failure is logged and swallowed. Nothing about
 * correctness depends on this class working.
 */
class CommonWellDocumentedCache {
	private static final Logger LOGGER = Logger.getLogger(CommonWellDocumentedCache.class.getName());

	/**
	 * Overrides the cache directory (default: {@code ~/.dash-cache/cwd}). Same spirit as
	 * {@link org.dash.valid.ars.AntigenRecognitionSiteCache#ARS_CACHE_DIR_PROPERTY} -- an edge-case
	 * escape hatch, not something normal use requires setting.
	 */
	public static final String CWD_CACHE_DIR_PROPERTY = "org.dash.cwdCacheDir";

	private static final String CACHED_AT_PREFIX = "#cachedAt=";
	private static final long LATEST_TTL_MILLIS = TimeUnit.HOURS.toMillis(24);

	private CommonWellDocumentedCache() {
	}

	/**
	 * @param hladb the hladb this data would be for (never null -- callers already default it)
	 * @return the cached accession map, or {@code null} on any cache miss
	 */
	static HashMap<String, List<String>> read(String hladb) {
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

			HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
			String line;
			while ((line = reader.readLine()) != null) {
				int tab = line.indexOf('\t');
				if (tab < 0) {
					continue;
				}

				String name = line.substring(0, tab);
				String accessionsJoined = line.substring(tab + 1);
				List<String> accessions = new ArrayList<String>();
				// An entry with no accessions at all is stored as an empty string --
				// "".split(",") returns [""], not [], so this must be checked explicitly, same
				// edge case as AntigenRecognitionSiteCache's empty allele sets.
				if (!accessionsJoined.isEmpty()) {
					accessions.addAll(Arrays.asList(accessionsJoined.split(",")));
				}
				accessionMap.put(name, accessions);
			}

			return accessionMap;
		}
		catch (Exception e) {
			LOGGER.info("Ignoring unreadable CWD accession cache file " + file + ": " + e);
			return null;
		}
	}

	/**
	 * Best-effort write; any failure is logged and swallowed rather than propagated.
	 *
	 * @param hladb the hladb this data is for
	 * @param accessionMap the freshly-parsed map to persist
	 */
	static void write(String hladb, HashMap<String, List<String>> accessionMap) {
		File dir = cacheDir();
		File tempFile = null;

		try {
			Files.createDirectories(dir.toPath());

			// Created directly inside the target directory so the Files.move() below is
			// guaranteed to be on the same filesystem -- required for ATOMIC_MOVE to actually be
			// atomic rather than silently falling back to a non-atomic copy-then-delete.
			tempFile = File.createTempFile("cwd-", ".tmp", dir);

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
				writer.write(CACHED_AT_PREFIX + System.currentTimeMillis());
				writer.newLine();

				for (Map.Entry<String, List<String>> entry : accessionMap.entrySet()) {
					// GLStringUtilities#convertToProteinLevel (whose result loadFromIMGT uses as
					// the map key) can return null for a malformed/short allele name -- confirmed
					// against real hla.xml.zip data, not just a theoretical case. write(null)
					// throws NPE. Skipping the (rare, already-anomalous) null-keyed entry is a
					// fine trade to keep the rest of a real map cacheable -- and the tempFile
					// cleanup in the catch below means even an entry that fails for some other,
					// unanticipated reason won't leave permanent debris either way.
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
			LOGGER.info("Couldn't write CWD accession cache for hladb " + hladb + ": " + e);
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
		// Matches the same normalization CommonWellDocumentedLoader#loadFromIMGT uses to build
		// the fetch URL, so the cache key exactly tracks whatever actually determines the
		// content. Sanitized defensively in case some future hladb value contains characters
		// that aren't safe in a filename.
		String normalized = hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING);
		String safeName = normalized.replaceAll("[^A-Za-z0-9_-]", "_");
		return new File(cacheDir(), "cwd-" + safeName + ".cache");
	}

	private static File cacheDir() {
		String override = System.getProperty(CWD_CACHE_DIR_PROPERTY);
		if (override != null && !override.isEmpty()) {
			return new File(override);
		}

		return new File(System.getProperty("user.home"), ".dash-cache" + File.separator + "cwd");
	}
}
