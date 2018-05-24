// created on May 4, 2018

/**
 * @author danielrowan
 */
#define HIST_BINS 256

kernel void Histogram(global const int* data,
                      global int* histogram,
                      int numData
                      )
{
    int lid = get_local_id(0);
    int gid = get_global_id(0);
    
    local int localHistogram[HIST_BINS];
        
    // init local histogram
    for(int i = lid; i < HIST_BINS; i+= get_local_size(0)) {
        localHistogram[i] = 0;
    }
    
    // calling barrier on intel CPU (i7-6920HQ) on MacOS will cause invalid work group size error:
    // https://stackoverflow.com/questions/26278448/using-a-barrier-causes-a-cl-invalid-work-group-size-error
    // wait for all work-items within group to complete init
    barrier(CLK_LOCAL_MEM_FENCE);

    // only init inside array bound
    if(gid < numData) {
        // clear the global histogram - need to do this in kernel for non-integrated GPUs
        for (int i = gid; i < numData; i+= get_global_size(0)) {
            histogram[i] = 0;
        }
    }

    barrier(CLK_LOCAL_MEM_FENCE);
    
    // compute local histogram
    if(gid < numData)
    {
        for(int i = gid; i < numData; i+= get_global_size(0)) {
            atomic_add(&localHistogram[data[i]], 1);
        }
    }
    // wait for all work-items within group to complete init
    barrier(CLK_LOCAL_MEM_FENCE);
    
    // write out the local histogram to global histogram
    for(int i = lid; i < HIST_BINS; i+= get_local_size(0)) {
        atomic_add(&histogram[i], localHistogram[i]);
    }
    //*/
}
