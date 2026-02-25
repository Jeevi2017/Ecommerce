package com.example.Ecomm.service;

import java.io.IOException;

import com.google.zxing.WriterException;

public interface IQRCodeService {

    byte[] generateQRCodeImage(String data, int width, int height)
            throws WriterException, IOException;

    byte[] generateQRCodeFromUrl(String url, int width, int height)
            throws WriterException, IOException;
}
