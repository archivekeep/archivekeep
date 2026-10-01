package org.archivekeep.app.core

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.smithy.kotlin.runtime.net.url.Url
import com.adobe.testing.s3mock.testcontainers.S3MockContainer

suspend fun createTestBucket(
    s3Mock: S3MockContainer,
    bucketName: String,
) {
    S3Client
        .fromEnvironment {
            endpointUrl = Url.parse(s3Mock.httpEndpoint)
            region = "us-east-1"
            credentialsProvider =
                StaticCredentialsProvider {
                    accessKeyId = "foo"
                    secretAccessKey = "bar"
                }
            forcePathStyle = true
        }.use { s3 ->
            try {
                s3.createBucket {
                    bucket = bucketName
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
}
