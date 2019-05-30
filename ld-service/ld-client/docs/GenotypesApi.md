# GenotypesApi

All URIs are relative to *https://virtserver.swaggerhub.com/mpresteg/HLAHapV/1.0.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**submitGenotypes**](GenotypesApi.md#submitGenotypes) | **POST** /genotypes | Submit a set of genotypes for evaluation


<a name="submitGenotypes"></a>
# **submitGenotypes**
> submitGenotypes(body)

Submit a set of genotypes for evaluation

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.GenotypesApi;


GenotypesApi apiInstance = new GenotypesApi();
Genotypes body = new Genotypes(); // Genotypes | Genotypes object that needs to be evaluated
try {
    apiInstance.submitGenotypes(body);
} catch (ApiException e) {
    System.err.println("Exception when calling GenotypesApi#submitGenotypes");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Genotypes**](Genotypes.md)| Genotypes object that needs to be evaluated |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json, application/xml
 - **Accept**: application/json, application/xml

