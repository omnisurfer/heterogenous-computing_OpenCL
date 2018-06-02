/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sandbox.heterogenous_computing;


import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import java.awt.Point;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import static java.lang.Math.*;
import static java.lang.System.nanoTime;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;

/**
 *
 * @author danielrowan
 */
public class ch4_2_histogram {
    
    //static CLContext context;
    //static CLDevice devices[];
    
    //int maxWorkGroupSize;
    static int HIST_BINS = 256;
    static int SELECTED_PLATFORM = 0;    
    
    public static void main(String[] args) throws Exception {

        System.out.println("ch4_2_histogram()");
              
        int maxWorkGroupSize;
        
        // <editor-fold defaultstate="collasped" desc="Step 1 & 2, get platform and devices and create a context">
        CLPlatform[] platform = CLPlatform.listCLPlatforms();
        
        System.out.println(platform[SELECTED_PLATFORM]);
        
        CLContext context = CLContext.create(platform[SELECTED_PLATFORM]);
    
        CLDevice devices[] = context.getDevices();
                
        int deviceIndex = SelectDevice.SelectDevice(context, devices);
        
        if(deviceIndex < 0) {
            System.out.println("Exiting...");
            context.release();
            return;
        }
        
        if(deviceIndex < 0) {
            System.out.println("Exiting...");
            context.release();
            return;
        }
           
        // </editor-fold>        
        
        // <editor-fold desc="Rad in Image Data">
        // https://www.mkyong.com/java/how-to-convert-bufferedimage-to-byte-in-java/
        // https://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
        // https://alvinalexander.com/blog/post/java/getting-rgb-values-for-each-pixel-in-image-using-java-bufferedi
        // https://stackoverflow.com/questions/3211156/how-to-convert-image-to-byte-array-in-java
        // https://stackoverflow.com/questions/15839223/java-awt-image-databufferbyte-cannot-be-cast-to-java-awt-image-databufferint
        // https://www.dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
        // https://stackoverflow.com/questions/46282213/bufferedimage-from-4-bit-data-buffer
        // https://blog.idrsolutions.com/2009/08/bufferedimage-raster-data-in-java/
        byte[] inputByteArray;
        int[] inputIntArray;
        DataBufferByte inputDataBufferByte;   
        BufferedImage inputBufferedImage;
        WritableRaster inputWritableRaster;
        
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
        
        //<editor-fold desc="Code example to write to a bitmap">        
        /* This code exists solely to sanity check that I am able to manipulate 
        * the bitmap correctly.
        */
        ///*
        //***DEBUG***
        // do stuff to the image to test file operations
        
        for(int i = 0; i < inputDataBufferByte.getSize(); i ++)
        {            
            int value = (inputDataBufferByte.getElem(i));
                        
            if(value < 127)
                value = 1;           
            else if (value >= 127)
                value = 255;
            
            inputDataBufferByte.setElem(i, value);
        }        
        
        WritableRaster outputWritableRaster = Raster.createPackedRaster(inputDataBufferByte, inputBufferedImage.getWidth(), inputBufferedImage.getHeight(), 8, new Point(0,0));
        
        BufferedImage outputBufferedImage = new BufferedImage(inputBufferedImage.getWidth(), inputBufferedImage.getHeight(), inputBufferedImage.getType());        
        outputBufferedImage.setData(outputWritableRaster);
                        
        try (FileOutputStream fos = new FileOutputStream("Resources/Images/catbuff.bmp")) {
        
            ImageIO.write(outputBufferedImage, "bmp", fos);
                       
            //fos.write(imageByteArray);   
            
            fos.close();
            
        } catch (IOException e) {
            
            e.printStackTrace();
            
        }
        //***DEBUG***        
        //*/
        //</editor-fold>
        
        // convert DataByteBuffer to something the compute kernel can use.
        inputByteArray = inputDataBufferByte.getData();
        
        inputIntArray = new int[inputByteArray.length];
        
        //https://stackoverflow.com/questions/6057530/how-to-convert-the-byte-array-into-an-integer-array?rq=1
        for(int i = 0; i < inputByteArray.length; i++)
        {
            inputIntArray[i] = inputDataBufferByte.getElem(i);
        }                
        
        // </editor-fold>
                        
        // <editor-fold defaultstate="collasped" desc="Step 3, creating a command queue">                
        
        maxWorkGroupSize = devices[deviceIndex].getMaxWorkGroupSize();                      
        
        CLCommandQueue queue = devices[deviceIndex].createCommandQueue();                           
        
        int elementCount = inputIntArray.length;                        
        
        // lenght of arrays to process
        int localWorkSize = min(maxWorkGroupSize, 1024);                 // local work size dimensions  
        int globalWorkSize = roundUp(localWorkSize, elementCount);      // rounded up to the nearest multiple of the localWorkSize, aka compute units available in the hardware.
        
        System.out.println("Selected Device: " + queue.getDevice().getName()
            + "\n\tdevice maxWorkGroupSize: " + maxWorkGroupSize
            + "\n\tcomputed localWorkSize: " + localWorkSize
            + "\n\tcomputed globalWorkSize: " + globalWorkSize
            + "\n\telementCount: " + elementCount
        );               
        
        // </editor-fold>
        
        // <editor-fold defaultstate="collasped" desc="Steps 6 & 7, load the cl kernel sources and build">
                
        // Note, JavaClass loader may have issues that make it return null no matter what:
        // http://www.onjava.com/2005/01/26/classloading.html
        // https://stackoverflow.com/questions/1921238/getclass-getclassloader-is-null-why  
        // Turns out VectorAdd.cl was not included in the target/classes forlder for the built application jar.
        // https://maven.apache.org/plugins/maven-resources-plugin/examples/include-exclude.html
        
        InputStream streamIn = ch4_2_histogram.class.getResourceAsStream("/Histogram.cl");              
                                    
        CLProgram program = context.createProgram(streamIn).build();
        
        // </editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Step 4, buffer operations">
        // determine histogram memory size
        int histogramSize = HIST_BINS;
        
        CLBuffer<IntBuffer> clBufferHistogram = context.createIntBuffer(histogramSize, WRITE_ONLY);                     
              
        // ready the buffers
        // clearing the buffer outside of the kernel does not work on discrete GPUs it seems.
        //clearBuffer(clBufferHistogram.getBuffer());        

        CLBuffer<IntBuffer> clBufferInputInts = context.createIntBuffer(globalWorkSize, READ_ONLY);
        
        System.out.println(
                "CLBuffInInt_elementSize (Bytes/elem): " + clBufferInputInts.getElementSize()
                + " CLBuffInInt_Size (Bytes): " + clBufferInputInts.getCLSize()
                + " CLBuffInInt_Capacity (Elems): " + clBufferInputInts.getCLCapacity()
                + " \ninputIntArray length (Elems): " + inputIntArray.length                
                + " inputIntArray (Bytes): " + inputIntArray.length * Integer.BYTES
        );
        
        fillBuffer(clBufferInputInts.getBuffer(), inputIntArray);
                        
        System.out.println("Used Device Memory: " 
            + (clBufferInputInts.getCLSize()+clBufferHistogram.getCLCapacity())/(1024*1024)
            + "MB"
            + "\ngWS/lWS = "
            + globalWorkSize/localWorkSize
        );
        //map buffers to kernel function 'Histogram'
        CLKernel kernel = program.createCLKernel("Histogram");
        kernel.putArgs(clBufferInputInts, clBufferHistogram).putArg(elementCount);                        
        
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Steps 8, write operations and kernel execution">
        
        //async write to GPU, with blocking read
        System.out.println("Processing...");
        long time = nanoTime();
        
        /* 
        For some reason localMemSize is 0MB on desktop. Maybe on Windows, graphics displays can't also be used for OpenCL?
        Also, maybe windows is reading the cl code incorrectly (long shot)?
        https://software.intel.com/en-us/forums/opencl/topic/697202
        */
        queue.putWriteBuffer(clBufferInputInts, true)
            .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
            .putReadBuffer(clBufferHistogram, true);
        
        time = nanoTime() - time;
               
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Step 9, reading data back to host">
        
        System.out.println("histogram results snapshot: ");        
        
        for(int i = 0; i < 256; i++)
            System.out.println(clBufferHistogram.getBuffer().get() + ",");
        
        int remaining = clBufferHistogram.getBuffer().remaining();
        
        System.out.println("...;" + remaining + " more");
        
        System.out.println("computation took: " + (time/10000000) + "ms");
        
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Step 10, release resources">        
        streamIn.close();
        
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
