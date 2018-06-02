/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sandbox.heterogenous_computing;


import static com.jogamp.common.nio.Buffers.newDirectFloatBuffer;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLImage;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.gl.CLGLImage2d;
import java.awt.Point;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import static java.lang.Math.*;
import static java.lang.System.nanoTime;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;

/**
 *
 * @author danielrowan
 */
public class ch4_3_rotation {
        
    public static void main(String[] args) throws Exception {

        System.out.println("ch4_3_rotation()");             
                
        // <editor-fold defaultstate="collasped" desc="Get platform and devices and create a context">
        
        int maxWorkGroupSize;
        
        CLContext context = CLContext.create();
        CLDevice devices[] = context.getDevices();
                
        int deviceIndex = SelectDevice.SelectDevice(context, devices);
        
        if(deviceIndex < 0) {
            System.out.println("Exiting...");
            context.release();
            return;
        }
           
        // </editor-fold>        
        
        // <editor-fold desc="Read in Image Data & Convert to Usable Format">
        // https://www.mkyong.com/java/how-to-convert-bufferedimage-to-byte-in-java/
        // https://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
        // https://alvinalexander.com/blog/post/java/getting-rgb-values-for-each-pixel-in-image-using-java-bufferedi
        // https://stackoverflow.com/questions/3211156/how-to-convert-image-to-byte-array-in-java
        // https://stackoverflow.com/questions/15839223/java-awt-image-databufferbyte-cannot-be-cast-to-java-awt-image-databufferint
        // https://www.dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
        // https://stackoverflow.com/questions/46282213/bufferedimage-from-4-bit-data-buffer
        // https://blog.idrsolutions.com/2009/08/bufferedimage-raster-data-in-java/
        byte[] inputByteArray;
        float[] inputFloatImageArray;
        float[] outputFloatImageArray;
        DataBufferByte inputDataBufferByte;
        DataBufferByte outputDataBufferByte;
        BufferedImage inputBufferedImage;
        WritableRaster inputWritableRaster;
        
        float angle = 90.0f;
        
        try (ByteArrayOutputStream boas = new ByteArrayOutputStream()) {
            
            File bmpFile = new File("Resources/Images/cat-face.bmp");
            inputBufferedImage = ImageIO.read(bmpFile);
                        
            inputWritableRaster = inputBufferedImage.getRaster();
            inputDataBufferByte = (DataBufferByte) inputWritableRaster.getDataBuffer();
            
            boas.close();         
            
        } catch (IOException e)
        {
            System.out.println(e.getMessage());
            return;
        }               
        
        // convert DataByteBuffer to something the compute kernel can use.
        inputByteArray = inputDataBufferByte.getData();
              
        ////convert into float array for processing operations
        //https://stackoverflow.com/questions/6057530/how-to-convert-the-byte-array-into-an-integer-array?rq=1
        
        inputFloatImageArray = new float[inputByteArray.length];
                
        for(int i = 0; i < inputByteArray.length; i ++)
        {
            inputFloatImageArray[i] = inputDataBufferByte.getElem(i);
        }
        
        // </editor-fold>
                        
        // <editor-fold defaultstate="collasped" desc="Create a command queue">                
        
        maxWorkGroupSize = devices[deviceIndex].getMaxWorkGroupSize();                      
        
        CLCommandQueue queue = devices[deviceIndex].createCommandQueue();                           
        
        int elementCount = inputFloatImageArray.length;                        
        
        // lenght of arrays to process
        int localWorkSize = min(maxWorkGroupSize, 128);                 // local work size dimensions  
        int globalWorkSize = roundUp(localWorkSize, elementCount);      // rounded up to the nearest multiple of the localWorkSize, aka compute units available in the hardware.
        
        System.out.println("Selected Device: " + queue.getDevice().getName()
            + "\n\tdevice maxWorkGroupSize: " + maxWorkGroupSize
            + "\n\tcomputed localWorkSize: " + localWorkSize
            + "\n\tcomputed globalWorkSize: " + globalWorkSize
            + "\n\telementCount: " + elementCount
        );                
        // </editor-fold>
        
        // <editor-fold defaultstate="collasped" desc="Load the cl kernel sources and build">
                
        // Note, JavaClass loader may have issues that make it return null no matter what:
        // http://www.onjava.com/2005/01/26/classloading.html
        // https://stackoverflow.com/questions/1921238/getclass-getclassloader-is-null-why  
        // Turns out VectorAdd.cl was not included in the target/classes forlder for the built application jar.
        // https://maven.apache.org/plugins/maven-resources-plugin/examples/include-exclude.html
        
        InputStream streamIn = ch4_3_rotation.class.getResourceAsStream("/Rotation.cl");              
                                    
        CLProgram program = context.createProgram(streamIn).build();
        
        // </editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Buffer operations">
        outputFloatImageArray = new float[inputFloatImageArray.length];                       
                                                                                   
        // <editor-fold desc="Image description setup">
        CLImageFormat clImageFormat = new CLImageFormat(CLImageFormat.ChannelOrder.R, CLImageFormat.ChannelType.FLOAT);
        
        
        //Search in jogamp forum: CLImage2D example program - Exception while executing kernel
        CLImage2d<FloatBuffer> inputImageMemory = context.createImage2d(
                newDirectFloatBuffer(inputFloatImageArray),
                inputBufferedImage.getWidth(),
                inputBufferedImage.getHeight(),
                clImageFormat);
                        
        CLImage2d<FloatBuffer> outputImageMemory = context.createImage2d(
                newDirectFloatBuffer(outputFloatImageArray),
                inputBufferedImage.getWidth(),
                inputBufferedImage.getHeight(),
                clImageFormat);
        
        
        
        //map buffers to kernel function 'Histogram'
        CLKernel kernel = program.createCLKernel("Rotation");
        kernel.putArg(inputImageMemory)
                .putArg(outputImageMemory)
                .putArg(inputBufferedImage.getWidth())
                .putArg(inputBufferedImage.getHeight())
                .putArg(angle);
        
        //</editor-fold>
        
        // </editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Kernel write operations and execution">
        
        //async write to GPU, with blocking read
        System.out.println("Processing...");
        long time = nanoTime();
                    
        queue.putWriteImage(inputImageMemory, true);
        
        queue.put2DRangeKernel(kernel, 0, 0, inputBufferedImage.getWidth(), inputBufferedImage.getHeight(), 0, 0);
        
        queue.putReadImage(outputImageMemory, true);
        
        time = nanoTime() - time;
               
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Kernel data read operations">
        System.out.println("computation took: " + (time/10000000) + "ms");
        
        System.out.println("Write out rotated image: ");
        
        //convert from clImageMemory back to a byte buffer
        FloatBuffer tempBuffer = outputImageMemory.getBuffer();
        byte[] tempByteBuffer = new byte[tempBuffer.capacity()];
        
        
        for(int i = 0; i < tempBuffer.capacity(); i ++)
        {
            outputFloatImageArray[i] = tempBuffer.get(i);
        }
                
        //convert float to byte                
        for(int i = 0; i < tempBuffer.capacity(); i++) {
            tempByteBuffer[i] = (byte)outputFloatImageArray[i];                             
        }
        
        outputDataBufferByte = new DataBufferByte(tempByteBuffer, tempByteBuffer.length);
                        
        //https://stackoverflow.com/questions/12705385/how-to-convert-a-byte-to-a-bufferedimage-in-java        
        BufferedImage outputBufferedImage;
                
        //<editor-fold desc="Code example to write to a bitmap">        
                  
        WritableRaster outputWritableRaster = Raster.createPackedRaster(outputDataBufferByte, inputBufferedImage.getWidth(), inputBufferedImage.getHeight(), 8, new Point(0,0));        
        
        outputBufferedImage = new BufferedImage(inputBufferedImage.getWidth(), inputBufferedImage.getHeight(), inputBufferedImage.getType());        
        outputBufferedImage.setData(outputWritableRaster);
                        
        try (FileOutputStream fos = new FileOutputStream("Resources/Images/rotation.bmp")) {
        
            ImageIO.write(outputBufferedImage, "bmp", fos);
                       
            //fos.write(imageByteArray);   
            
            fos.close();
            
        } catch (IOException e) {
            
            e.printStackTrace();
            
        }
        //***DEBUG***        
        //*/
        //</editor-fold>
        
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Release resources">        
        streamIn.close();
                             
        inputImageMemory.release();
        outputImageMemory.release();
        context.release();                             
        
        // </editor-fold>                
    }

    private static int roundUp(int groupSize, int globalSize) {
        
        int r = globalSize % groupSize;
        
        if (r == 0) {
            return globalSize;          
        } else {
            return globalSize + groupSize - r;
        }
    }
    
    private static void fillBuffer(IntBuffer buffer, int[] input)
    {
        int bufferDiff = buffer.capacity() - input.length;
        int i = 0;
        while(buffer.remaining() != bufferDiff)
        {                                   
            buffer.put(input[i]);
            i++;
        }
                
        //fill the remaining buffer positions with 0
        while(buffer.remaining() != 0)
        {                                   
            buffer.put(0);
            i++;
        }        
        buffer.rewind();
    }
    
    private static void clearBuffer(IntBuffer buffer)
    {
        while(buffer.remaining() != 0)
        {
            int value = 0;
            buffer.put(value);
        }
        
        buffer.rewind();
    }
    
}
