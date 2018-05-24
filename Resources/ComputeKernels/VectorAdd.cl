// created on May 1, 2018

/**
 * @author danielrowan
 */
kernel void VectorAdd(global const float* clBufferA, global const float* clBufferB, global float* clBufferC, int numElements) {
    
    // get index into global data array
    int iGID = get_global_id(0);
        
    // bound check similar to for loop limit
    if (iGID >= numElements) {
        clBufferC[iGID] = 0.0;
        return;
    }
    
    // perform vector addition
    clBufferC[iGID] = clBufferA[iGID] + clBufferB[iGID];
    
    /* Naive code for summing over the 1D vector
        - totally forgot to initialize the temp buffer.
     */
    /*
    float tempBuffer = 0.0f;
    int memWorkWindow = get_global_size(0)/get_local_size(0);
    */
    /*
    if(iGID == localWorkSize - 1)
        memWorkWindow = globalWorkSize - numElements;
    */
    /*
    for(int i = 0; i < numElements; ++i)
    {
        tempBuffer += clBufferA[i];
        
        // significanly more efficient global sum:
        // https://developer.apple.com/library/content/samplecode/OpenCL_Parallel_Reduction_Example/Listings/ReadMe_txt.html#//apple_ref/doc/uid/DTS40008188-ReadMe_txt-DontLinkElementID_3
     
        // clBufferC[iGID] += clBufferA[i];
    }
    clBufferC[iGID] = tempBuffer; //get_group_id(0); //get_global_id(0)/get_local_size(0); //get_local_id(0);
    */
}
