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
import static com.jogamp.opencl.CLDevice.Type.GPU;
import com.jogamp.opencl.CLKernel;
import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import static com.jogamp.opencl.util.CLPlatformFilters.type;

import java.io.InputStream;
import static java.lang.Math.*;
import static java.lang.System.nanoTime;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * References:
 * https://jogamp.org/wiki/index.php/JOCL_Tutorial
 * @author danielrowan
 * 
 * NVIDIA NOT SHOWING UP AS AN OPTION 
 * http://forum.jogamp.org/CL-DEVICE-NOT-AVAILABLE-when-creating-CLContext-with-64-bit-Java-on-GTX-970-td4036560.html#a4036566
 * 
 * http://forum.jogamp.org/template/NamlServlet.jtp?macro=search_page&node=762907&query=nvidia+jocl&days=0&sort=date
 */
public class ch3_6_example {
    
    //static CLContext context;
    //static CLDevice devices[];
    //static int maxWorkGroupSize;    
    static int SELECTED_DEVICE = 0;
    
    public static void main(String[] args) throws Exception {

        System.out.println("ch3_6_example()");             
         
        int maxWorkGroupSize;  
        
        // <editor-fold defaultstate="collasped" desc="Step 1 & 2, get platform and devices and create a context">
        CLPlatform[] platform = CLPlatform.listCLPlatforms();
        
        System.out.println(platform[SELECTED_DEVICE]);
        
        CLContext context = CLContext.create(platform[SELECTED_DEVICE]);
    
        CLDevice devices[] = context.getDevices();
                              
        int deviceIndex = SelectDevice.SelectDevice(context, devices);
        
        if(deviceIndex < 0) {
            System.out.println("Exiting...");
            context.release();
            return;
        }
           
        // </editor-fold>
        
        // <editor-fold defaultstate="collasped" desc="Step 3, creating a command queue">
                
        maxWorkGroupSize = devices[deviceIndex].getMaxWorkGroupSize();        
        CLCommandQueue queue = devices[deviceIndex].createCommandQueue();                           
                        
        int elementCount = 1024 * 800; //777600;             
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
        
        // <editor-fold defaultstate="collasped" desc="Steps 6 & 7, load the cl kernel sources and build">
                
        // Note, JavaClass loader may have issues that make it return null no matter what:
        // http://www.onjava.com/2005/01/26/classloading.html
        // https://stackoverflow.com/questions/1921238/getclass-getclassloader-is-null-why  
        // Turns out VectorAdd.cl was not included in the target/classes forlder for the built application jar.
        // https://maven.apache.org/plugins/maven-resources-plugin/examples/include-exclude.html
        
        InputStream streamIn = ch3_6_example.class.getResourceAsStream("/VectorAdd.cl");                
                                    
        CLProgram program = context.createProgram(streamIn).build();
        
        // </editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Step 4, buffer operations">
        
        // A, B are input, C is for results
        CLBuffer<FloatBuffer> clBufferA = context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferB = context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferC = context.createFloatBuffer(globalWorkSize, WRITE_ONLY);
        
        System.out.println(
            "Used Device Memory: " 
                + (clBufferA.getCLSize()+clBufferB.getCLSize()+clBufferC.getCLCapacity())/(1024*1024)
                + "MB"
                + "\ngWS/lWS = "
                + globalWorkSize/localWorkSize
        );
        
        // fill inputs with random numbers
        fillBuffer(clBufferA.getBuffer(), 12345);
        fillBuffer(clBufferB.getBuffer(), 67890);
        clearBuffer(clBufferC.getBuffer());
        
        //map buffers to kernel function 'VectorAdd'
        CLKernel kernel = program.createCLKernel("VectorAdd");
        kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);  
        
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Steps 8, write operations and kernel execution">
        
        //async write to GPU, with blocking read
        System.out.println("Processing...");
        long time = nanoTime();
        
        queue.putWriteBuffer(clBufferA, true)
            .putWriteBuffer(clBufferB, true)
            .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
            .putReadBuffer(clBufferC, true);
        
        time = nanoTime() - time;
        
        //</editor-fold>
        
        //<editor-fold defaultstate="collasped" desc="Step 9, reading data back to host">
        
        System.out.println("a+b=c results snapshot: ");        
        
        for(int i = 0; i < 10; i++)
            System.out.println(clBufferC.getBuffer().get() + ",");
        
        int remaining = clBufferC.getBuffer().remaining();
        
        System.out.println("...;" + remaining + " more");
             
        /*
        int tailValues = localWorkSize * 1;
        int tailIndex = clBufferC.getBuffer().capacity() - tailValues;
        
        for (int i = 0; i < tailValues; i++)
            System.out.println(clBufferC.getBuffer().get(tailIndex + i));
        
        System.out.println("...last " + tailValues);
        */
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
        
    private static void fillBuffer(FloatBuffer buffer, int seed)
    {
        int i = 0;
        Random rnd = new Random(seed);
        while(buffer.remaining() != 0)
        {
            //buffer.put(rnd.nextFloat()*1000);
            buffer.put(++i);
        }
        buffer.rewind();
    }
    
    private static void clearBuffer(FloatBuffer buffer)
    {
        while(buffer.remaining() != 0)
            buffer.put(0);
        
        buffer.rewind();
    }
}
