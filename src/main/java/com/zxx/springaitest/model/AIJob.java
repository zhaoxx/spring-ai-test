package com.zxx.springaitest.model;

public class AIJob {

     public record Job(JobType jobType, String orderNo, String name) {

         
     }

     public enum JobType {
         CANCEL,
         QUERY,
         OTHER
     }
}
