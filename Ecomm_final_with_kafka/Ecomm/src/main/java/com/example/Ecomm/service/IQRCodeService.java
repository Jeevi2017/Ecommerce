package com.example.Ecomm.service;


public interface IQRCodeService {
    byte[] generateQRCodeImage(String data, int width, int height) throws Exception;
    byte[] generateQRCodeFromUrl(String url, int width, int height) throws Exception;

}
