//
//  PhotoCaptionInputViewPlugin.m
//  Helper
//
//  Created by James Kong on 21/04/2017.
//
//

#import "PhotoCaptionInputViewPlugin.h"
#import <Cordova/CDVViewController.h>
#import "MBProgressHUD.h"
#import "MRProgress.h"
#import "MWPhotoExt.h"
#import "MWCommon.h"
#import "CustomViewController.h"
#import "Masonry.h"
#define LIGHT_BLUE_COLOR [UIColor colorWithRed:(99/255.0f)  green:(176/255.0f)  blue:(228.0f/255.0f) alpha:1.0]
#define BUNDLE_UIIMAGE(imageNames) [UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), imageNames]]
#define BIN_UIIMAGE BUNDLE_UIIMAGE(@"images/bin.png")
#define SENDFRIEND_UIIMAGE BUNDLE_UIIMAGE(@"images/sendfriend.png")
#define KEY_LABEL @"label"
#define KEY_TYPE @"type"

#define KEY_FRIEND @"friend"
#define KEY_PLAYLIST @"playlist"
#define KEY_ALBUM @"album"
#define TEXT_SIZE 16
@implementation PhotoCaptionInputViewPlugin

@synthesize callbackId;
@synthesize callbackIds = _callbackIds;
@synthesize photos = _photos;
@synthesize thumbnails = _thumbnails;
@synthesize photoCaptionInputViewController = _photoCaptionInputViewController;
@synthesize buttonOptions = _buttonOptions;
@synthesize distinationType = _distinationType;
- (NSMutableDictionary*)callbackIds {
    if(_callbackIds == nil) {
        _callbackIds = [[NSMutableDictionary alloc] init];
        
    }
    return _callbackIds;
}

- (void)showCaptionInput:(CDVInvokedUrlCommand*)command {
#ifdef DEBUG
    NSLog(@"showCaptionInput:%@", command.arguments);
#endif
    self.callbackId = command.callbackId;
    [self.callbackIds setValue:command.callbackId forKey:@"showGallery"];
    
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSMutableArray *images = [[NSMutableArray alloc] init];
    NSMutableArray *thumbs = [[NSMutableArray alloc] init];
    NSInteger maximumImagesCount = [[options objectForKey:@"maximumImagesCount"] integerValue];
    self.maximumImagesCount = (maximumImagesCount == 0 ) ? 100 : maximumImagesCount;
    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    self.width = [[options objectForKey:@"width"] integerValue];
    self.height = [[options objectForKey:@"height"] integerValue];
    if([options objectForKey:@"quality"] != nil){
        self.quality = [[options objectForKey:@"quality"] integerValue];
    }else{
        self.quality = 100;
    }
    
    //    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    self.preSelectedAssets = [options objectForKey:@"preSelectedAssets"];
    
    NSDictionary * data = [options objectForKey:@"data"];
    NSArray *argfriends = [options objectForKey:@"friends"];
    
    
    _distinationType = @"";
    if(![[argfriends class] isEqual:[NSNull class]]){
        self.friends = argfriends;
    }else{
        self.friends = [NSArray new];
        
    }
    
    
    NSArray *buttonOptions = [options objectForKey:@"buttons"];
    if(![[buttonOptions class] isEqual:[NSNull class]]){
        _buttonOptions = [NSMutableArray arrayWithArray:buttonOptions];
    }
#ifdef DEBUG
    NSLog(@"data %@",data);
#endif
    UIScreen *screen = [UIScreen mainScreen];
    CGFloat scale = screen.scale;
    // Sizing is very rough... more thought required in a real implementation
    CGFloat imageSize = MAX(screen.bounds.size.width, screen.bounds.size.height) * 1.5;
    CGSize imageTargetSize = CGSizeMake(imageSize * scale, imageSize * scale);
    CGSize thumbTargetSize = CGSizeMake(imageSize / 3.0 * scale, imageSize / 3.0 * scale);
    
    
    [self.preSelectedAssets enumerateObjectsUsingBlock:^(NSString * _Nonnull localIdentifier, NSUInteger idx, BOOL * _Nonnull stop) {
        
        PHFetchResult<PHAsset *> * result = [PHAsset fetchAssetsWithLocalIdentifiers:@[localIdentifier] options:nil];
        PHAsset *asset = [result objectAtIndex:0];
        [images addObject:[MWPhotoExt photoWithAsset:asset targetSize:imageTargetSize] ];
        [thumbs addObject:[MWPhotoExt photoWithAsset:asset targetSize:thumbTargetSize] ];
    }];
    
    self.photos = images;
    self.thumbnails = thumbs;
    
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:_photos thumbnails:_thumbnails preselectedAssets:self.preSelectedAssets delegate:self];
    vc.alwaysShowControls = YES;
    vc.maximumImagesCount = self.maximumImagesCount;
    _photoCaptionInputViewController = vc;
    
    CustomViewController *nc = [[CustomViewController alloc]initWithRootViewController:vc];
    CATransition *transition = [[CATransition alloc] init];
    transition.duration = 0.35;
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromRight;
    [[UINavigationBar appearance] setBarTintColor:[UIColor clearColor]];
    [[UINavigationBar appearance] setTranslucent:YES];
    [self.viewController.view.window.layer addAnimation:transition forKey:kCATransition];
    
    [self.viewController presentViewController:nc animated:NO completion:^{
    }];
    
    
}

-(void) onSendPressed:(id) sender{
    UIButton *button = (UIButton*)sender;
    [_buttonOptions enumerateObjectsUsingBlock:^(NSDictionary *obj, NSUInteger idx, BOOL * _Nonnull stop) {
        
        if([[obj valueForKey:KEY_LABEL] isEqualToString:button.titleLabel.text]){
            
            _distinationType = [obj valueForKey:KEY_TYPE];
        }
    }];
    [_photoCaptionInputViewController getPhotosCaptions];
}

#pragma mark - PhotoCaptionInputViewDelegate


-(void) dismissPhotoCaptionInputView:(PhotoCaptionInputViewController*)controller{
    CDVPluginResult* pluginResult = nil;
    NSString *message = [NSString stringWithFormat:@"No Result in PhotoCaptionInputViewPlugin (%@) ", NSLocalizedString(@"USER_CANCELLED", nil)];
#ifdef DEBUG
    NSLog(@"%@", message);
#endif
    
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[NSDictionary new]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
#ifdef DEBUG
    NSLog(@"PhotoCaptionInputView: User pressed cancel button");
#endif
    
    [controller dismissViewControllerAnimated:YES completion:nil];
    
}
-(void) photoCaptionInputView:(PhotoCaptionInputViewController*)controller captions:(NSArray *)captions photos:(NSArray*)inPhotos preSelectedAssets:(NSArray*)preselectAssets{
    
    
    __block NSMutableArray *preSelectedAssets = [[NSMutableArray alloc] init];
    __block NSMutableArray *fileStrings = [[NSMutableArray alloc] init];
    
    
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
    requestOptions.synchronous = false;
    
    dispatch_group_t dispatchGroup = dispatch_group_create();
    MRProgressOverlayView *progressView = [MRProgressOverlayView new];
    progressView.mode = MRProgressOverlayViewModeDeterminateCircular;
    progressView.tintColor = [UIColor darkGrayColor];
    progressView.titleLabel.attributedText = [[NSAttributedString alloc] initWithString:NSLocalizedString(@"LOADING", nil)
                                                                             attributes:@{
                                                                                          NSForegroundColorAttributeName: UIColor.blackColor,
                                                                                          NSFontAttributeName: [UIFont preferredFontForTextStyle:UIFontTextStyleHeadline],
                                                                                          NSKernAttributeName: NSNull.null,  // turn on auto-kerning
                                                                                          }];
    [progressView show:YES];
    [controller.view addSubview:progressView];
    [progressView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.centerY.equalTo(progressView.superview.mas_centerY);
        make.centerX.equalTo(progressView.superview.mas_centerX);
    }];
    dispatch_group_async(dispatchGroup, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        //        PHFetchResult<PHAsset *> * fetchArray = [PHAsset fetchAssetsWithLocalIdentifiers:inPhotos options:nil];
        __block CGFloat numberOfImage = [inPhotos count];
        [self processAssets:inPhotos
                      index:0
                   docsPath:docsPath
                 targetSize:targetSize
                    manager:manager
             requestOptions:requestOptions
              startEndTimes:nil
          completedCallback:^() {
              if (nil == result) {
                  result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", _distinationType, KEY_TYPE, nil]];
              }
              dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
                  
                  [progressView dismiss:YES];
                  [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
                  [controller.presentingViewController dismissViewControllerAnimated:YES completion:nil];
                  [self.viewController dismissViewControllerAnimated:YES completion:nil];
                  
                  
              });
              
          } nextCallback:^(NSInteger index, NSString *filePath, NSString *localIdentifier, NSString *invalidImage) {
              if(invalidImage != nil){
                  [fileStrings addObject:@""];
                  [preSelectedAssets addObject: @""];
                  [invalidImages addObject: invalidImage];
              }else{
                  [fileStrings addObject:filePath];
                  [preSelectedAssets addObject: localIdentifier];
                  [invalidImages addObject: @""];
              }
              dispatch_async(dispatch_get_main_queue(), ^{
                  [progressView setProgress:(CGFloat)index/(CGFloat)numberOfImage];
              });
              
          } errorCallback:^(CDVPluginResult *errorResult) {
              dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
                  [progressView dismiss:YES];
                  [self.commandDelegate sendPluginResult:errorResult callbackId:self.callbackId];
                  [controller.presentingViewController dismissViewControllerAnimated:YES completion:nil];
                  [self.viewController dismissViewControllerAnimated:YES completion:nil];
              });
          }];
        
    });
    
}

-(void) processAssets:(NSArray*)fetchAssets
                index:(NSInteger)index
             docsPath:(NSString*)docsPath
           targetSize:(CGSize)targetSize
              manager:(PHImageManager*)manager
       requestOptions:(PHImageRequestOptions *)requestOptions
        startEndTimes:(NSArray*)startEndTimes
    completedCallback:(void(^)(void))completedCallback
         nextCallback:(void(^)(NSInteger index , NSString* filePath, NSString* localIdentifier, NSString* invalidImage))nextCallback
        errorCallback:(void(^)(CDVPluginResult* errorResult))errorCallback{
    
    if(index >= [fetchAssets count]){
        completedCallback();
        return;
    }
    
    __block NSInteger internalIndex = index;
    NSString *assetString = [fetchAssets objectAtIndex:internalIndex];
    PHFetchResult<PHAsset*> *fetchResult = [PHAsset fetchAssetsWithLocalIdentifiers: @[assetString] options:nil];
    PHAsset *asset = [fetchResult objectAtIndex:0];
    NSString *localIdentifier;
    NSError * err;
    CDVPluginResult *result = nil;
    if (asset == nil) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
        errorCallback(result);
    } else if(asset.mediaType == PHAssetMediaTypeImage){
        localIdentifier = [asset localIdentifier];
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"yyyyMMddHHmmss"];
        NSString *localizedDateString = [dateFormatter stringFromDate:[NSDate date]];
        NSString* fileName = [[localIdentifier componentsSeparatedByString:@"/"] objectAtIndex:0] ;
        NSString *filePath = [NSString stringWithFormat:@"%@/%@-%@.%@", docsPath, fileName, localizedDateString, @"jpg"];
        __block UIImage *image;
        localIdentifier = [asset localIdentifier];
        if([[NSFileManager defaultManager] fileExistsAtPath:filePath]){
            internalIndex++;
            nextCallback(internalIndex,[[NSURL fileURLWithPath:filePath] absoluteString], localIdentifier, nil);
            [self processAssets:fetchAssets
                          index:internalIndex
                       docsPath:docsPath
                     targetSize:targetSize
                        manager:manager
                 requestOptions:requestOptions
                  startEndTimes:nil
              completedCallback:completedCallback nextCallback:nextCallback errorCallback:errorCallback];
        }else{
            
            
            [manager requestImageDataForAsset:asset
                                      options:requestOptions
                                resultHandler:^(NSData *imageData,
                                                NSString *dataUTI,
                                                UIImageOrientation orientation,
                                                NSDictionary *info) {
#ifdef DEBUG
                                    NSLog(@"info %@", info);
                                    
                                    NSLog(@"ExifData %@", [self getExifDataFromImageData:imageData]);
#endif
                                    if([dataUTI isEqualToString:@"public.png"] || [dataUTI isEqualToString:@"public.jpeg"] || [dataUTI isEqualToString:@"public.jpeg-2000"] || [dataUTI isEqualToString:@"public.heic"]) {
                                        
                                        
                                        if (imageData != nil) {
                                            
                                            @autoreleasepool {
                                                // Save off the properties
                                                
                                                CGImageSourceRef imageSource = CGImageSourceCreateWithData((__bridge CFDataRef) imageData, NULL);
                                                
                                                
                                                
                                                NSMutableDictionary *imageMetadata = [(NSDictionary *) CFBridgingRelease(CGImageSourceCopyPropertiesAtIndex(imageSource, 0, NULL)) mutableCopy];
                                                CFRelease(imageSource);
                                                
                                                
                                                NSData* data = nil;
                                                if (self.width == 0 && self.height == 0) {
                                                    image = [UIImage imageWithData:imageData];
                                                    data = (imageMetadata != NULL)? [self writeMetadataIntoImageData:UIImageJPEGRepresentation(image, self.quality/100.0f) metadata: [[NSMutableDictionary alloc]initWithDictionary:imageMetadata]] : UIImageJPEGRepresentation(image, self.quality/100.0f) ;
                                                } else {
                                                    image = [UIImage imageWithData:imageData];
                                                    // scale
                                                    UIImage* scaledImage = [self imageByScalingNotCroppingForSize:image toSize:targetSize];
                                                    if (imageMetadata != NULL) {
                                                        NSMutableDictionary *metaData = [[NSMutableDictionary alloc]initWithDictionary:imageMetadata];
                                                        NSMutableDictionary *TIFF = [metaData objectForKey:@"{TIFF}"];
                                                        [TIFF setValue:0 forKey:@"Orientation"];
                                                        [metaData setValue:0 forKey:@"Orientation"];
                                                        [metaData setValue:TIFF forKey:@"{TIFF}"];
                                                        data = [self writeMetadataIntoImageData:UIImageJPEGRepresentation(scaledImage, self.quality/100.0f) metadata: metaData];
                                                    } else{
                                                        data = UIImageJPEGRepresentation(scaledImage, self.quality/100.0f) ;
                                                    }
                                                    
                                                    
                                                }
                                                
                                                NSError *err;
                                                if (![data writeToFile:filePath options:NSAtomicWrite error:&err] ) {
                                                    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                                    errorCallback(result);
                                                } else {
                                                    internalIndex++;
                                                    nextCallback(internalIndex,[[NSURL fileURLWithPath:filePath] absoluteString], localIdentifier, nil);
                                                    [self processAssets:fetchAssets
                                                                  index:internalIndex
                                                               docsPath:docsPath
                                                             targetSize:targetSize
                                                                manager:manager
                                                         requestOptions:requestOptions
                                                          startEndTimes:nil
                                                      completedCallback:completedCallback nextCallback:nextCallback errorCallback:errorCallback];
                                                }
                                                
                                            }
                                            
                                        }else{
                                            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                            errorCallback(result);
                                        }
                                        
                                    } else {
                                        
                                        internalIndex ++;
                                        nextCallback(internalIndex,nil, nil, localIdentifier);
                                        [self processAssets:fetchAssets
                                                      index:internalIndex
                                                   docsPath:docsPath
                                                 targetSize:targetSize
                                                    manager:manager
                                             requestOptions:requestOptions
                                              startEndTimes:nil
                                          completedCallback:completedCallback nextCallback:nextCallback errorCallback:errorCallback];
                                    }
                                }];
            
            
        }
    }
    
}


- (NSMutableArray*)photoBrowser:(MWPhotoBrowser *)photoBrowser buildToolbarItems:(UIToolbar*)toolBar{
    NSMutableArray *items = [[NSMutableArray alloc] init];
    
    UIBarButtonItem *flexSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:self action:nil];
    [items addObject:flexSpace];
    float margin = 0.0f;
    
    
    
    
    [toolBar setBackgroundImage:[UIImage new]
             forToolbarPosition:UIToolbarPositionAny
                     barMetrics:UIBarMetricsDefault];
    
    [toolBar setBackgroundColor:[UIColor clearColor]];
    toolBar.clipsToBounds = YES;
    for (UIView *subView in [toolBar subviews]) {
        if ([subView isKindOfClass:[UIImageView class]]) {
            // Hide the hairline border
            subView.hidden = YES;
        }
    }
    //    BOOL hasFriend = YES;
    if([_buttonOptions count] ==1 ){
        NSDictionary * dic = [_buttonOptions objectAtIndex:0];
        CGRect newFrame = CGRectMake(0,0,
                                     self.viewController.view.frame.size.width - 10,
                                     44 );
        UIButton *button = [[UIButton alloc] initWithFrame: newFrame];
        [button setBackgroundColor:LIGHT_BLUE_COLOR];
        button.layer.cornerRadius = 2; // this value vary as per your desire
        button.clipsToBounds = YES;
        [button setAttributedTitle:[self attributedString:[dic valueForKey:KEY_LABEL] WithSize:TEXT_SIZE color:[UIColor whiteColor]] forState:UIControlStateNormal];
        [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
        
        UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:button];
        [items addObject:addPhotoButton];
    }else{
        
        [_buttonOptions enumerateObjectsUsingBlock:^(NSDictionary *obj, NSUInteger idx, BOOL * _Nonnull stop) {
            
            CGFloat  buttonWidth = (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"11.0")) ? (self.viewController.view.frame.size.width *.45)-10 : (self.viewController.view.frame.size.width *.5) - 10;
            
            NSString *labelText = [obj valueForKey:KEY_LABEL];
            if (idx ==0 ) {
                UIButton *button =  [UIButton buttonWithType:UIButtonTypeCustom];
                [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
                CGRect newFrame = CGRectMake(0,0,
                                             buttonWidth,
                                             44 );
                [button setFrame:newFrame];
                [button setBackgroundColor:LIGHT_BLUE_COLOR];
                button.layer.cornerRadius = 2; // this value vary as per your desire
                button.clipsToBounds = YES;
                [button setAttributedTitle:[self attributedString:labelText WithSize:TEXT_SIZE color:[UIColor whiteColor]] forState:UIControlStateNormal];
                button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
                
                UIBarButtonItem *addFriendsButton = [[UIBarButtonItem alloc] initWithCustomView:button];
                [items addObject:addFriendsButton];
                
                
            }else{
                
                CGRect newFrame = CGRectMake(0,0,
                                             buttonWidth,
                                             44 );
                UIButton *button = [[UIButton alloc] initWithFrame: newFrame];
                [button setBackgroundColor:LIGHT_BLUE_COLOR];
                button.layer.cornerRadius = 2; // this value vary as per your desire
                button.clipsToBounds = YES;
                [button setAttributedTitle:[self attributedString:labelText WithSize:TEXT_SIZE color:[UIColor whiteColor]] forState:UIControlStateNormal];
                [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
                button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
                UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:button];
                [items addObject:addPhotoButton];
                
            }
            if(idx != [_buttonOptions count]-1){
                UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
                if(SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"11.0")){
                    fixedSpace.width = 1;
                }else{
                    fixedSpace.width = -8;
                }
                [items addObject:fixedSpace];
                
            }
        }];
        
        
    }
    [items addObject:flexSpace];
    //    [items addObject:fixedSpace];
    //    UIBarButtonItem *fixedSpace2 = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
    //    fixedSpace2.width = 15;
    //    [items addObject:fixedSpace2];
    toolBar.barStyle = UIBarStyleDefault;
    
    toolBar.barTintColor = [UIColor whiteColor];;
    return items;
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
#ifdef DEBUG
        NSLog(@"could not scale image");
#endif
    }
    
    // pop the context to get back to the default
    UIGraphicsEndImageContext();
    return newImage;
}

-(NSAttributedString *) attributedString:(NSString*)string WithSize:(NSInteger)size color:(UIColor*)color{
    NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc]init];
    
    NSDictionary *dictAttr0 = [self attributedDirectoryWithSize:size color:color];
    NSAttributedString *attr0 = [[NSAttributedString alloc]initWithString:string attributes:dictAttr0];
    [attributedString appendAttributedString:attr0];
    return attributedString;
}

-(NSDictionary *) attributedDirectoryWithSize:(NSInteger)size color:(UIColor*)color{
    NSDictionary *dictAttr0 = @{NSFontAttributeName:[UIFont systemFontOfSize:size],
                                NSForegroundColorAttributeName:color};
    return dictAttr0;
}


-(NSDictionary*)getExifDataFromImageData:(NSData*) imageData
{
    @try {
        
        CGImageSourceRef imageSource = CGImageSourceCreateWithData((__bridge CFDataRef)imageData, nil);
        if (imageSource != NULL)
        {
            NSDictionary *metaoptions = @{(NSString *)kCGImageSourceShouldCache : [NSNumber numberWithBool:NO]};
            
            CFDictionaryRef imageProperties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, (__bridge CFDictionaryRef)metaoptions);
            NSDictionary *myMetadata = [NSDictionary dictionaryWithDictionary:(__bridge NSDictionary *)imageProperties];
            
            CFRelease(imageProperties);
            CFRelease(imageSource);
            
            return myMetadata;
            
        }
    }@catch(NSException *exception){
#ifdef DEBUG
        NSLog(@"Error: %@",exception);
#endif
    }
    return NULL;
}
// Newly implemented method. saveImage is not working
- (BOOL) writeImage:(UIImage *)img withOptions:(NSDictionary *) options {
    NSString *fullFilePath =  [options objectForKey:@"fullFilePath"];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];
    NSDictionary *meta = [options objectForKey:@"meta"] ;
    
    NSData *data = (meta != NULL)? [self writeMetadataIntoImageData:UIImageJPEGRepresentation(img, quality/100.0f) metadata: [[NSMutableDictionary alloc]initWithDictionary:meta]] : UIImageJPEGRepresentation(img, quality/100.0f) ;
    NSError* err = nil;
    if (![data writeToFile:fullFilePath options:NSAtomicWrite error:&err]) {
        return NO;
    } else {
        return YES;
    }
    
}


//http://stackoverflow.com/questions/9006759/how-to-write-exif-metadata-to-an-image-not-the-camera-roll-just-a-uiimage-or-j
-(NSData *)writeMetadataIntoImageData:(NSData *)imageData metadata:(NSMutableDictionary *)metadata {
    // create an imagesourceref
    CGImageSourceRef source = CGImageSourceCreateWithData((__bridge CFDataRef) imageData, NULL);
    
    // this is the type of image (e.g., public.jpeg)
    CFStringRef UTI = CGImageSourceGetType(source);
    
    // create a new data object and write the new image into it
    NSMutableData *dest_data = [NSMutableData data];
    CGImageDestinationRef destination = CGImageDestinationCreateWithData((__bridge CFMutableDataRef)dest_data, UTI, 1, NULL);
    if (!destination) {
#ifdef DEBUG
        NSLog(@"Error: Could not create image destination");
#endif
    }
    // add the image contained in the image source to the destination, overidding the old metadata with our modified metadata
    CGImageDestinationAddImageFromSource(destination, source, 0, (__bridge CFDictionaryRef) metadata);
    BOOL success = NO;
    success = CGImageDestinationFinalize(destination);
    if (!success) {
#ifdef DEBUG
        NSLog(@"Error: Could not create data from image destination");
#endif
    }
    CFRelease(destination);
    CFRelease(source);
    return dest_data;
}


@end

