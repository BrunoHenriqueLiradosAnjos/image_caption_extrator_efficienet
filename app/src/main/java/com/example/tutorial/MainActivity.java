package com.example.tutorial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;


import org.tensorflow.lite.DataType;


public class MainActivity extends AppCompatActivity {
    static final int DIM_IMG_SIZE_X = 512;
    static final int DIM_IMG_SIZE_Y = 512;
    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 3;
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private static final int CAMERA_REQUEST = 1888;
    ImageView imageView;
    TextView textoDescricao;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    Interpreter tflite;

    Bitmap imagePredict;

    private ByteBuffer imgData = null;


    //model_extrator_feacture.tflite

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.button);
        //TextView textoDescricao = findViewById(R.id.textView);
        this.imageView = (ImageView)this.findViewById(R.id.imageView);
        this.textoDescricao = this.findViewById(R.id.textView);

        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);



                //Toast.makeText(MainActivity.this, "Olá Mundo", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void convertBitmapToByteBuffer(Bitmap imagePredict) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        imagePredict.getPixels(intValues, 0, imagePredict.getWidth(), 0, 0, imagePredict.getWidth(), imagePredict.getHeight());
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");




            imageView.setImageBitmap(photo);

           // TextView textoDescricao = findViewById(R.id.textView);
            //textoDescricao.setText("Descrição Boladona OffDoom");
            convertBitmapToByteBuffer(photo);
           // float inferenceValor = labelProbArray[0][0];

            //textoDescricao.setText(Float.toString(inferenceValor));

            if(imgData!=null){
                //327680
                //327679
                float[] labelProbArray = new float[327680];
                try {
                    this.tflite = new Interpreter(loadModelFile());
                } catch (Exception ex){
                    ex.printStackTrace();
                }

                tflite.run(imgData, labelProbArray);
                tflite.close();
                //float inferenceValor = labelProbArray[0];
                String output = "";
                for (int i = 0; i < 10; i++) {
                    output = output+ "||";
                    output = output+Float.toString(labelProbArray[i]).substring(0,8);

                }



                textoDescricao.setText(output);

            }




        }
    }
    private MappedByteBuffer loadModelFile() throws IOException{
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_extrator_feacture.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
}