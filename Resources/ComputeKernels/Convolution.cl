// created on June 21, 2018

/**
 * @author danielrowan
 */

__constant sampler_t sampler =
CLK_NORMALIZED_COORDS_FALSE |
CLK_FILTER_LINEAR           |
CLK_ADDRESS_CLAMP;

kernel void Convolution(
                    __read_only image2d_t inputImage,
                    __write_only image2d_t outputImage,
                    global float* filter,
                    int imageWidth,
                    int imageHeight,
                    int filterWidth
                    )
{
    
    float filterTemp[25] = {
            1.0f/273.0f, 4.0f/273.0f, 7.0f/273.0f, 4.0f/273.0f, 1.0f/273.0f,
            4.0f/273.0f, 16.0f/273.0f, 26.0f/273.0f, 16.0f/273.0f, 4.0f/273.0f,
            7.0f/273.0f, 26.0f/273.0f, 41.0f/273.0f, 26.0f/273.0f, 7.0f/273.0f,
            4.0f/273.0f, 26.0f/273.0f, 26.0f/273.0f, 16.0f/273.0f, 4.0f/273.0f,
            1.0f/273.0f, 4.0f/273.0f, 7.0f/273.0f, 4.0f/273.0f, 1.0f/273.0f
        };
    
    float x[2] = { 0.8f, 0.1f };
    
    int column = get_global_id(0);
    int row = get_global_id(1);
    
    int halfWidth = (int)(filterWidth/2);
    
    float4 sum = {0.0f, 0.0f, 0.0f, 0.0f};
    
    int filterIdx = 0;
    
    int2 coords;
    
    for(int i = -halfWidth; i <= halfWidth; i++)
    {
        coords.y = row + i;
        
        for(int j = -halfWidth; j <= halfWidth; j++)
        {
            coords.x = column + j;
            
            float4 pixel;
            
            pixel = read_imagef(inputImage, sampler, coords);
            //sum.x = pixel.x * filter[];
            sum.x += pixel.x * filter[filterIdx++]; //filterTemp[filterIdx++];
        }
    }
    
    coords.x = column;
    coords.y = row;
    
    write_imagef(outputImage, coords, sum);
    //write_imagef(outputImage, (int2)(column, row), (float4)(10.0f, 0.0f, 0.0f, 0.0f));
}

