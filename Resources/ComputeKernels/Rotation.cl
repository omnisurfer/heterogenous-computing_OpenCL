// created on May 4, 2018

/**
 * @author danielrowan
 */

__constant sampler_t sampler = 
    CLK_NORMALIZED_COORDS_FALSE |
    CLK_FILTER_LINEAR           |
    CLK_ADDRESS_CLAMP;

kernel void Rotation(
        __read_only image2d_t inputImage,
        __write_only image2d_t outputImage,
        int imageWidth,
        int imageHeight,
        float theta
    )
{

    int x = get_global_id(0);
    int y = get_global_id(1);
 
    float x0 = imageWidth/2.0f;
    float y0 = imageHeight/2.0f;
    
    int xprime = x - x0;
    int yprime = y - y0;
    
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);
    
    float2 readCoord;
    readCoord.x = xprime*cosTheta - yprime*sinTheta + x0;
    readCoord.y = xprime*sinTheta + yprime*cosTheta + y0;
    
    float value;
    value = read_imagef(inputImage, sampler, readCoord).x;
    //value = read_imagef(inputImage, sampler, (int2)(x,y)).x;
    
    write_imagef(outputImage, (int2)(x,y), (float4)(value, 0.0f, 0.0f, 0.0f));
}
