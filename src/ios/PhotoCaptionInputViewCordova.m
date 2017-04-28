//
//  PhotoCaptionInputViewCordova.m
//  Helper
//
//  Created by James Kong on 21/04/2017.
//
//

#import "PhotoCaptionInputViewCordova.h"
#import <Cordova/CDVViewController.h>
#import "MBProgressHUD.h"
#import "MWPhotoExt.h"
@implementation PhotoCaptionInputViewCordova

@synthesize callbackId;
@synthesize callbackIds = _callbackIds;
@synthesize photos = _photos;
@synthesize thumbnails = _thumbnails;
@synthesize photoCaptionInputViewController = _photoCaptionInputViewController;
- (NSMutableDictionary*)callbackIds {
    if(_callbackIds == nil) {
        _callbackIds = [[NSMutableDictionary alloc] init];
        
    }
    return _callbackIds;
}

- (void)showCaptionInput:(CDVInvokedUrlCommand*)command {
    NSLog(@"showCaptionInput:%@", command.arguments);
    self.callbackId = command.callbackId;
    [self.callbackIds setValue:command.callbackId forKey:@"showGallery"];
    
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSMutableArray *images = [[NSMutableArray alloc] init];
    NSMutableArray *thumbs = [[NSMutableArray alloc] init];
    
    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    self.width = [[options objectForKey:@"width"] integerValue];
    self.height = [[options objectForKey:@"height"] integerValue];
    self.quality = [[options objectForKey:@"quality"] integerValue];
    
    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
        self.preSelectedAssets = [options objectForKey:@"preSelectedAssets"];
    
    NSDictionary * data = [options objectForKey:@"data"];
    
    NSLog(@"data %@",data);
    
    for (NSString* url in [options objectForKey:@"images"])
    {
        [images addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    }
    for (NSString* url in [options objectForKey:@"thumbnails"])
    {
        [thumbs addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    }
    
    self.photos = images;
    if([thumbs count] == 0){
        self.thumbnails = images;
    }else{
        self.thumbnails = thumbs;
    }
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:_photos thumbnails:_thumbnails preselectedAssets:self.preSelectedAssets delegate:self];
    _photoCaptionInputViewController = vc;
    UINavigationController *nc = [[UINavigationController alloc]initWithRootViewController:vc];
//    [self.viewController presentViewController:nc animated:YES completion:^{
//        
//    }];
    CATransition *transition = [[CATransition alloc] init];
    transition.duration = 0.35;
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromRight;
    [self.viewController.view.window.layer addAnimation:transition forKey:kCATransition];
    
    
    
    UIButton *button = [[UIButton alloc]initWithFrame:
                        CGRectMake(self.viewController.view.frame.size.width-60,
                                   self.viewController.view.frame.size.height-150,
                                   50,
                                   50)];
    [button setImage:[UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), @"images/send.png"]] forState:UIControlStateNormal];
//    [button setTitle:@"send" forState:UIControlStateNormal];
    button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleLeftMargin;
    [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
    [nc.view addSubview:button];
    
    [self.viewController presentViewController:nc animated:NO completion:^{
        
    }];

    
}

-(void) onSendPressed:(id) sender{
    [_photoCaptionInputViewController getPhotosCaptions];
}
#pragma mark - PhotoCaptionInputViewDelegate


-(void) dismissPhotoCaptionInputView:(PhotoCaptionInputViewController*)controller{
    CDVPluginResult* pluginResult = nil;
    NSArray* emptyArray = [NSArray array];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:emptyArray];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];

    NSLog(@"PhotoCaptionInputView: User pressed cancel button");
    

    [controller dismissViewControllerAnimated:YES completion:nil];

}
-(void) photoCaptionInputView:(PhotoCaptionInputViewController*)controller captions:(NSArray *)captions photos:(NSArray*)inPhotos preSelectedAssets:(NSArray*)preselectAssets{

    [controller.presentingViewController dismissViewControllerAnimated:YES completion:nil];
    
    NSLog(@"GMImagePicker: User finished picking assets. Number of selected items is: %lu", (unsigned long)photos.count);
    
    __block NSMutableArray *preSelectedAssets = [[NSMutableArray alloc] init];
    __block NSMutableArray *fileStrings = [[NSMutableArray alloc] init];
//    __block NSMutableArray *livePhotoFileStrings = [[NSMutableArray alloc] init];
    
    __block NSMutableArray *invalidImages = [[NSMutableArray alloc] init];
    CGSize targetSize = CGSizeMake(self.width, self.height);
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
    
    __block CDVPluginResult* result = nil;
    
    PHImageManager *manager = [PHImageManager defaultManager];
    PHImageRequestOptions *requestOptions;
    requestOptions = [[PHImageRequestOptions alloc] init];
    requestOptions.resizeMode   = PHImageRequestOptionsResizeModeExact;
    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
    requestOptions.networkAccessAllowed = YES;
    
    // this one is key
    requestOptions.synchronous = true;
    
    dispatch_group_t dispatchGroup = dispatch_group_create();
    
    MBProgressHUD *progressHUD = [MBProgressHUD showHUDAddedTo:self.viewController.view
                                                      animated:YES];
    progressHUD.mode = MBProgressHUDModeIndeterminate;
    progressHUD.dimBackground = YES;
    progressHUD.labelText = NSLocalizedStringFromTable(
                                                       @"picker.selection.downloading",
                                                       @"GMImagePicker",
                                                       @"iCloudLoading"
                                                       );
    [progressHUD show: YES];
    
    dispatch_group_async(dispatchGroup, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        __block NSString* filePath;
        NSError* err = nil;
        __block NSData *imgData;
        // Index for tracking the current image
        __block int index = 0;
        // If image fetching fails then retry 3 times before giving up
        PHFetchResult<PHAsset *> * featchArray = [PHAsset fetchAssetsWithLocalIdentifiers:preselectAssets options:nil];
        do {
            
            PHAsset *asset = [featchArray objectAtIndex:index];
            NSString *localIdentifier;
            
            
            if (asset == nil) {
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
            } else {
                __block UIImage *image;
                localIdentifier = [asset localIdentifier];
                NSLog(@"localIdentifier: %@", localIdentifier);
                NSString* fileName = [[localIdentifier componentsSeparatedByString:@"/"] objectAtIndex:0];
                filePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, fileName, @"jpg"];
                if([[NSFileManager defaultManager] fileExistsAtPath:filePath]){
                    [fileStrings addObject:[[NSURL fileURLWithPath:filePath] absoluteString]];
                    [preSelectedAssets addObject: localIdentifier];
                    index++;
                }else{
                    [manager requestImageDataForAsset:asset
                                              options:requestOptions
                                        resultHandler:^(NSData *imageData,
                                                        NSString *dataUTI,
                                                        UIImageOrientation orientation,
                                                        NSDictionary *info) {
                                            if([dataUTI isEqualToString:@"public.png"] || [dataUTI isEqualToString:@"public.jpeg"] || [dataUTI isEqualToString:@"public.jpeg-2000"]) {
                                                imgData = [imageData copy];
                                                NSString* fullFilePath = [info objectForKey:@"PHImageFileURLKey"];
                                                NSLog(@"fullFilePath: %@: " , fullFilePath);
                                                
                                            } else {
                                                imgData = nil;
                                                [invalidImages addObject: localIdentifier];
                                                index++;
                                            }
                                        }];
                    
                    
                    requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeFastFormat;
                    
                    if (imgData != nil) {
                        requestOptions.deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat;
                        @autoreleasepool {
                            NSData* data = nil;
                            if (self.width == 0 && self.height == 0) {
                                // no scaling required
                                if (self.quality == 100) {
                                    data = [imgData copy];
                                } else {
                                    image = [UIImage imageWithData:imgData];
                                    // resample first
                                    data = UIImageJPEGRepresentation(image, self.quality/100.0f);
                                }
                            } else {
                                image = [UIImage imageWithData:imgData];
                                // scale
                                UIImage* scaledImage = [self imageByScalingNotCroppingForSize:image toSize:targetSize];
                                data = UIImageJPEGRepresentation(scaledImage, self.quality/100.0f);
                            }
                            
                            if (![data writeToFile:filePath options:NSAtomicWrite error:&err] ) {
                                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                break;
                            } else {
                                [fileStrings addObject:[[NSURL fileURLWithPath:filePath] absoluteString]];
                                [preSelectedAssets addObject: localIdentifier];
                            }
                            data = nil;
                        }
                        
                    }else{
                        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                    }
                    index++;
                }
                
            }
        } while (index < featchArray.count);
        
        if (result == nil) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions", invalidImages, @"invalidImages", nil]];
        }
    });
    
    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
        if (nil == result) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", nil]];
        }
        
        progressHUD.progress = 1.f;
        [progressHUD hide:YES];
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });


    
}



- (UIImage*)imageByScalingNotCroppingForSize:(UIImage*)anImage toSize:(CGSize)frameSize
{
    UIImage* sourceImage = anImage;
    UIImage* newImage = nil;
    CGSize imageSize = sourceImage.size;
    CGFloat width = imageSize.width;
    CGFloat height = imageSize.height;
    CGFloat targetWidth = frameSize.width;
    CGFloat targetHeight = frameSize.height;
    CGFloat scaleFactor = 0.0;
    CGSize scaledSize = frameSize;
    
    if (CGSizeEqualToSize(imageSize, frameSize) == NO) {
        CGFloat widthFactor = targetWidth / width;
        CGFloat heightFactor = targetHeight / height;
        
        // opposite comparison to imageByScalingAndCroppingForSize in order to contain the image within the given bounds
        if (widthFactor == 0.0) {
            scaleFactor = heightFactor;
        } else if (heightFactor == 0.0) {
            scaleFactor = widthFactor;
        } else if (widthFactor > heightFactor) {
            scaleFactor = heightFactor; // scale to fit height
        } else {
            scaleFactor = widthFactor; // scale to fit width
        }
        scaledSize = CGSizeMake(floor(width * scaleFactor), floor(height * scaleFactor));
    }
    
    UIGraphicsBeginImageContext(scaledSize); // this will resize
    
    [sourceImage drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];
    
    newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
    }
    
    // pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}


@end
