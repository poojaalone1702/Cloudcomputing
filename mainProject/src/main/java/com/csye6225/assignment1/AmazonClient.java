package com.csye6225.assignment1;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class AmazonClient {

    private AmazonS3 s3client;

//    @Value("${amazonProperties.endpointUrl}")
//    private String endpointUrl;
//
//    @Value("${aws.s3.bucket}")
//    private String bucketName;


//    @Value("${profile.name}")
//    private String profilename;
//
//    public String getProfilename() {
//        return profilename;
//    }
//
//    public void setProfilename(String profilename) {
//        this.profilename = profilename;
//    }

    @PostConstruct
    private void initializeAmazon() {
        s3client = AmazonS3ClientBuilder.defaultClient();
    }



    public void uploadFileTos3bucket(String bn,String fileName, File file) {
        s3client.putObject(new PutObjectRequest(bn, fileName, file)
                .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));
    }



    public String deleteFileFromS3Bucket(String bucket,String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(new DeleteObjectRequest(bucket, fileName));
        return "Successfully deleted";
    }
}