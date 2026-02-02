package com.example.Ecomm.serviceImpl;

import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;

import com.example.Ecomm.service.IQRCodeService;

@Service
public class QRCodeServiceImpl implements IQRCodeService {

	@Override
    public byte[] generateQRCodeImage(String data, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            );
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new Exception("Could not generate QR code image", e);
        }
    }
	
	 @Override
	    public byte[] generateQRCodeFromUrl(String url, int width, int height) throws Exception {
	        return generateQRCodeImage(url, width, height);
	    }
}
