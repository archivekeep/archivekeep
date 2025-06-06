package org.archivekeep.app.core

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.smithy.kotlin.runtime.net.url.Url
import org.testcontainers.containers.MinIOContainer

suspend fun createTestBucket(
    minio: MinIOContainer,
    bucketName: String,
) {
    S3Client
        .fromEnvironment {
            endpointUrl = Url.parse(minio.s3URL)
            region = "TODO"
            credentialsProvider =
                StaticCredentialsProvider {
                    accessKeyId = minio.userName
                    secretAccessKey = minio.password
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
