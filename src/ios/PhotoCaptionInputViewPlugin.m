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
//#import "MRProgress.h"
#import "MWPhotoExt.h"
#import "MWCommon.h"
#import "CustomViewController.h"

#import "Masonry.h"
#import "FileHash.h"
@import Photos;
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
#define DEFUALT_VIDEO_LENGTH 15
@implementation PhotoCaptionInputViewPlugin

@synthesize callbackId;
@synthesize callbackIds = _callbackIds;
@synthesize photos = _photos;
@synthesize thumbnails = _thumbnails;
@synthesize photoCaptionInputViewController = _photoCaptionInputViewController;
@synthesize buttonOptions = _buttonOptions;
@synthesize destinationType = _destinationType;
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
    NSInteger maximumImagesCount = [[options objectForKey:@"maximumImagesCount"] integerValue];
    self.maximumImagesCount = (maximumImagesCount == 0 ) ? 100 : maximumImagesCount;
    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    self.width = [[options objectForKey:@"width"] integerValue];
    self.height = [[options objectForKey:@"height"] integerValue];
    self.quality = [[options objectForKey:@"quality"] integerValue];
    self.allow_video = [[options objectForKey:@"allow_video" ] boolValue ];
    //    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    self.preSelectedAssets = [options objectForKey:@"preSelectedAssets"];
    
    NSDictionary * data = [options objectForKey:@"data"];
    NSArray *argfriends = [options objectForKey:@"friends"];
    
    
    _destinationType = @"";
    if(![[argfriends class] isEqual:[NSNull class]]){
        self.friends = argfriends;
    }else{
        self.friends = [NSArray new];
        
    }
    
    
    NSArray *buttonOptions = [options objectForKey:@"buttons"];
    if(![[buttonOptions class] isEqual:[NSNull class]]){
        _buttonOptions = [NSMutableArray arrayWithArray:buttonOptions];
    }
    
    NSLog(@"data %@",data);
    UIScreen *screen = [UIScreen mainScreen];
    CGFloat scale = screen.scale;
    // Sizing is very rough... more thought required in a real implementation
    CGFloat imageSize = MAX(screen.bounds.size.width, screen.bounds.size.height) * 1.5;
    CGSize imageTargetSize = CGSizeMake(imageSize * scale, imageSize * scale);
    CGSize thumbTargetSize = CGSizeMake(imageSize / 3.0 * scale, imageSize / 3.0 * scale);
    
    PHFetchResult<PHAsset *> * result = [PHAsset fetchAssetsWithLocalIdentifiers:self.preSelectedAssets options:nil];
    NSInteger numVideos = 0;
    [result enumerateObjectsUsingBlock:^(PHAsset * _Nonnull asset, NSUInteger idx, BOOL * _Nonnull stop) {
        [images addObject:[MWPhotoExt photoWithAsset:asset targetSize:imageTargetSize] ];
        [thumbs addObject:[MWPhotoExt photoWithAsset:asset targetSize:thumbTargetSize] ];
    }];
    
    self.photos = images;
    self.thumbnails = thumbs;
    
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:_photos thumbnails:_thumbnails preselectedAssets:self.preSelectedAssets delegate:self];
    vc.allow_video = self.allow_video;
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
            
            _destinationType = [obj valueForKey:KEY_TYPE];
        }
    }];
    [_photoCaptionInputViewController getPhotosCaptions];
}

#pragma mark - PhotoCaptionInputViewDelegate


-(void) dismissPhotoCaptionInputView:(PhotoCaptionInputViewController*)controller{
    CDVPluginResult* pluginResult = nil;
    NSString *message = [NSString stringWithFormat:@"No Result in PhotoCaptionInputViewPlugin (%@) ", NSLocalizedString(@"USER_CANCELLED", nil)];
    NSLog(@"%@", message);
    
    
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[NSDictionary new]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    
    NSLog(@"PhotoCaptionInputView: User pressed cancel button");
    
    
    [controller dismissViewControllerAnimated:YES completion:nil];
    
}
-(void) photoCaptionInputView:(PhotoCaptionInputViewController*)controller captions:(NSArray *)captions photos:(NSArray*)photos preSelectedAssets:(NSArray*)preselectAssets startEndTime:(NSArray*)startEndTimes {
    [controller tilePages];
    __block NSMutableArray *preSelectedAssets = [[NSMutableArray alloc] init];
    __block NSMutableArray *fileStrings = [[NSMutableArray alloc] init];
    __block NSMutableArray *metaDatas = [[NSMutableArray alloc] init];
    
    
    __block NSMutableArray *invalidImages = [[NSMutableArray alloc] init];
    CGSize targetSize = CGSizeMake(self.width, self.height);
//    NSString* docsPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString* docsPath = [NSTemporaryDirectory() stringByStandardizingPath];
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
//    __block MRProgressOverlayView *progressView = [MRProgressOverlayView new];
//    progressView.mode = MRProgressOverlayViewModeDeterminateCircular;
//    progressView.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.3];
//    progressView.tintColor = [UIColor darkGrayColor];
//    [progressView show:YES];
    MBProgressHUD *hud = [MBProgressHUD showHUDAddedTo:controller.view animated:YES];
    hud.mode = MBProgressHUDModeAnnularDeterminate;
    hud.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.3];
    hud.label.text = @"Loading";
    
//    [controller.view addSubview:progressView];
//    [progressView mas_makeConstraints:^(MASConstraintMaker *make) {
//        make.centerY.equalTo(progressView.superview.mas_centerY);
//        make.centerX.equalTo(progressView.superview.mas_centerX);
//    }];
    dispatch_group_async(dispatchGroup, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        PHFetchResult<PHAsset *> * fetchArray = [PHAsset fetchAssetsWithLocalIdentifiers:preselectAssets options:nil];
        __block CGFloat numberOfImage = [fetchArray count];
        [self processAssets:fetchArray
                      index:0
                   docsPath:docsPath
                 targetSize:targetSize
                    manager:manager
             requestOptions:requestOptions
              startEndTimes:startEndTimes
          completedCallback:^() {
              controller.view.userInteractionEnabled = YES;
              if (nil == result) {
                  result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", metaDatas, @"metaDatas", _destinationType, KEY_TYPE, nil]];
              }
              dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
                  
//                  [progressView dismiss:YES];
                  [hud hideAnimated:YES];
                  [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
                  [controller.presentingViewController dismissViewControllerAnimated:YES completion:nil];
                  [self.viewController dismissViewControllerAnimated:YES completion:nil];
                  
                  
              });
              
          } nextCallback:^(NSInteger index, NSString *filePath, NSString *localIdentifier, NSString *_Nullable invalidImage, NSDictionary * _Nullable metadata) {
              if(invalidImage != nil){
                  [fileStrings addObject:@""];
                  [preSelectedAssets addObject: @""];
                  [invalidImages addObject: invalidImage];
              }else{
                  [fileStrings addObject:filePath];
                  [preSelectedAssets addObject: localIdentifier];
                  [invalidImages addObject: @""];
              }
              [metaDatas addObject:(metadata == nil)?@{}:metadata];
              
              dispatch_async(dispatch_get_main_queue(), ^{
//                  [progressView setProgress:(CGFloat)index/(CGFloat)numberOfImage];
                  hud.progress = (CGFloat)index/(CGFloat)numberOfImage;
              });
              
          } progressCallback:^(NSInteger index , CGFloat progress){
              dispatch_async(dispatch_get_main_queue(), ^{
//                  [progressView setProgress:((CGFloat)index/(CGFloat)numberOfImage)+((progress/100.0f)*(1/(CGFloat)numberOfImage))];
                  float outputprogress = ((CGFloat)index/(CGFloat)numberOfImage)+((progress/100.0f)*(1/(CGFloat)numberOfImage));
                  hud.label.text = [NSString stringWithFormat:@"%.f%%",outputprogress*100];
                  hud.progress = outputprogress;
              });
          } errorCallback:^(CDVPluginResult *errorResult) {
              dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
//                  [progressView dismiss:YES];
                  [hud hideAnimated:YES];
                  [self.commandDelegate sendPluginResult:errorResult callbackId:self.callbackId];
                  [controller.presentingViewController dismissViewControllerAnimated:YES completion:nil];
                  [self.viewController dismissViewControllerAnimated:YES completion:nil];
              });
          }];
        
    });
    
}

-(void) processAssets:(PHFetchResult<PHAsset *>*)fetchAssets
                index:(NSInteger)index
             docsPath:(NSString*)docsPath
           targetSize:(CGSize)targetSize
              manager:(PHImageManager*)manager
       requestOptions:(PHImageRequestOptions *)requestOptions
        startEndTimes:(NSArray*)startEndTimes
    completedCallback:(void(^)(void))completedCallback
         nextCallback:(void(^)(NSInteger index , NSString* filePath, NSString* localIdentifier, NSString* _Nullable invalidImage, NSDictionary *_Nullable metadata))nextCallback
     progressCallback:(void(^)(NSInteger index , CGFloat progress))progressCallback
        errorCallback:(void(^)(CDVPluginResult* errorResult))errorCallback{
    if(index >= [fetchAssets count]){
        completedCallback();
        return;
    }
    
    __block NSInteger internalIndex = index;
    PHAsset *asset = [fetchAssets objectAtIndex:internalIndex];
    
    NSString *localIdentifier;
    NSError * err;
    CDVPluginResult *result = nil;
    
    if (asset == nil) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
        errorCallback(result);
    } else if(asset.mediaType == PHAssetMediaTypeImage){
        localIdentifier = [asset localIdentifier];
        NSString* fileName = [[localIdentifier componentsSeparatedByString:@"/"] objectAtIndex:0] ;
        NSString *filePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, fileName, @"jpg"];
        __block UIImage *image;
        localIdentifier = [asset localIdentifier];
        if([[NSFileManager defaultManager] fileExistsAtPath:filePath]){
            internalIndex++;
            nextCallback(internalIndex,[[NSURL fileURLWithPath:filePath] absoluteString], localIdentifier, nil, nil);
            [self processAssets:fetchAssets
                          index:internalIndex
                       docsPath:docsPath
                     targetSize:targetSize
                        manager:manager
                 requestOptions:requestOptions
                  startEndTimes:nil
              completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
        }else{
            [manager requestImageDataForAsset:asset
                                      options:requestOptions
                                resultHandler:^(NSData *imageData,
                                                NSString *dataUTI,
                                                UIImageOrientation orientation,
                                                NSDictionary *info) {
                                    if([dataUTI isEqualToString:@"public.png"] || [dataUTI isEqualToString:@"public.jpeg"] || [dataUTI isEqualToString:@"public.jpeg-2000"] || [dataUTI isEqualToString:@"public.heic"]) {
                                        
                                        
                                        if (imageData != nil) {
                                            
                                            @autoreleasepool {
                                                NSData* data = nil;
                                                if (self.width == 0 && self.height == 0) {
                                                    // no scaling required
                                                    if (self.quality == 100) {
                                                        data = [imageData copy];
                                                    } else {
                                                        image = [UIImage imageWithData:imageData];
                                                        // resample first
                                                        data = UIImageJPEGRepresentation(image, self.quality/100.0f);
                                                    }
                                                } else {
                                                    image = [UIImage imageWithData:imageData];
                                                    // scale
                                                    UIImage* scaledImage = [self imageByScalingNotCroppingForSize:image toSize:targetSize];
                                                    data = UIImageJPEGRepresentation(scaledImage, self.quality/100.0f);
                                                }
                                                
                                                NSError *err;
                                                if (![data writeToFile:filePath options:NSAtomicWrite error:&err] ) {
                                                    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                                    errorCallback(result);
                                                } else {
                                                    ;
                                                    internalIndex++;
                                                    nextCallback(internalIndex,[[NSURL fileURLWithPath:[self getMD5FileString:filePath docPath:docsPath subtype:@".jpg"]] absoluteString], localIdentifier, nil, nil);
                                                    [self processAssets:fetchAssets
                                                                  index:internalIndex
                                                               docsPath:docsPath
                                                             targetSize:targetSize
                                                                manager:manager
                                                         requestOptions:requestOptions
                                                          startEndTimes:nil
                                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                                }
                                                
                                            }
                                            
                                        }else{
                                            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                                            errorCallback(result);
                                        }
                                        
                                    } else {
                                        
                                        internalIndex ++;
                                        nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                                        [self processAssets:fetchAssets
                                                      index:internalIndex
                                                   docsPath:docsPath
                                                 targetSize:targetSize
                                                    manager:manager
                                             requestOptions:requestOptions
                                              startEndTimes:nil
                                          completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                    }
                                }];
            
            
        }
    } else if(asset.mediaType == PHAssetMediaTypeVideo){
        localIdentifier = [asset localIdentifier];
        NSLog(@"localIdentifier: %@", localIdentifier);
        CGFloat startTime = [[[startEndTimes objectAtIndex:internalIndex] valueForKey:@"startTime"] floatValue];
        CGFloat endTime = [[[startEndTimes objectAtIndex:internalIndex] valueForKey:@"endTime"] floatValue];
        __block NSString *edited = @"false";
        if([[startEndTimes objectAtIndex:internalIndex] valueForKey:@"auto"] != nil){
            edited = @"auto";
        }
        NSString* fileName = [[localIdentifier componentsSeparatedByString:@"/"] objectAtIndex:0];
        NSString* outputExtension = @".mp4";
        __block NSString *filePath = [NSString stringWithFormat:@"%@/%@.%@", docsPath, fileName, @"mp4"];
        
        {
            if([[NSFileManager defaultManager] fileExistsAtPath:filePath]){
                if([[NSFileManager defaultManager] removeItemAtPath:filePath error:&err]){
                    NSLog(@"file  exist now delete");
                }
            }
            PHVideoRequestOptions *options = [PHVideoRequestOptions new];
            options.networkAccessAllowed = YES;
            
            [manager requestAVAssetForVideo:asset options:options resultHandler:^(AVAsset * _Nullable avasset, AVAudioMix * _Nullable audioMix, NSDictionary * _Nullable info) {
                NSArray* compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:avasset];
                if ([avasset isReadable]) {
                    NSLog(@"The asset is readable, ok.");
                }
                if ([avasset isKindOfClass:[AVURLAsset class]] && [compatiblePresets containsObject:AVAssetExportPresetMediumQuality]) {
                    
                    NSURL *furl = [NSURL fileURLWithPath:filePath];
                    CMTime start = CMTimeMakeWithSeconds(startTime, avasset.duration.timescale);
                    CMTime tempDuration = CMTimeMakeWithSeconds(endTime - startTime, avasset.duration.timescale);
                    float tempDurationSecond = CMTimeGetSeconds(tempDuration);
                    float assetDuration = CMTimeGetSeconds(avasset.duration);
                    CMTime fifteen = CMTimeMakeWithSeconds( DEFUALT_VIDEO_LENGTH, avasset.duration.timescale);
                    CMTime duration = (tempDurationSecond > 1 && tempDurationSecond < DEFUALT_VIDEO_LENGTH) ? tempDuration : (assetDuration < DEFUALT_VIDEO_LENGTH ) ? avasset.duration : fifteen;
                    
                    if(![edited isEqualToString:@"auto"]){
                        edited = (CMTimeGetSeconds(start)==0 && CMTimeGetSeconds(duration)==DEFUALT_VIDEO_LENGTH) ? @"false" : @"true";
                    }
                    
                    CMTimeRange range = CMTimeRangeMake(start, duration);
                    NSLog(@"start = %f duration= %f", CMTimeGetSeconds(start),CMTimeGetSeconds(duration));
                    NSArray *tracks = [avasset tracksWithMediaType:AVMediaTypeVideo];
                    AVAssetTrack *track = [tracks objectAtIndex:0];
                    CGSize mediaSize = track.naturalSize;

//                    @"duration": duration,
//                    @"width": width,
//                    @"height": height,
//                    @"originalDuration": originalDuration,
//                    @"edited":edited
                    
                    __block NSDictionary *metaDic = @{
                                                      @"duration": @((int)((CMTimeGetSeconds(duration)*1000))),
                                                      @"width": @((int)mediaSize.width),
                                                      @"height":@((int)mediaSize.height),
                                                      @"originalDuration":@((int)(CMTimeGetSeconds(avasset.duration)*1000)),
                                                      @"edited":edited
                                                      };
                    
                    AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:avasset presetName:AVAssetExportPresetMediumQuality];
                    exportSession.outputURL = furl;
                
                    exportSession.outputFileType=AVFileTypeQuickTimeMovie;
                    exportSession.timeRange = range;
                    exportSession.shouldOptimizeForNetworkUse = NO;
                    
                    dispatch_semaphore_t sessionWaitSemaphore = dispatch_semaphore_create(0);
                    
                    void (^completionHandler)(void) = ^(void)
                    {
                        dispatch_semaphore_signal(sessionWaitSemaphore);
                    };
                    
                    // do it
                    [self.commandDelegate runInBackground:^{
                        [exportSession exportAsynchronouslyWithCompletionHandler:completionHandler];
                        do {
                            dispatch_time_t dispatchTime = DISPATCH_TIME_FOREVER;  // if we dont want progress, we will wait until it finishes.
                            dispatchTime = getDispatchTimeFromSeconds((float)1.0);
                            double progress = [exportSession progress] * 100;
                            
                            progressCallback(internalIndex, progress);
                            dispatch_semaphore_wait(sessionWaitSemaphore, dispatchTime);
                        } while( [exportSession status] < AVAssetExportSessionStatusCompleted );
                        
                        switch ([exportSession status]) {
                            case AVAssetExportSessionStatusFailed:
                                
                                NSLog(@"Export failed: %@", [[exportSession error] localizedDescription]);
                                
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                break;
                            case AVAssetExportSessionStatusCancelled:
                                
                                NSLog(@"Export canceled %@", [[exportSession error] localizedDescription]);
                                
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                break;
                            case AVAssetExportSessionStatusWaiting:
                                NSLog(@"Export waiting");
                                break;
                            case AVAssetExportSessionStatusExporting:
                                NSLog(@"Export exporting");
                                break;
                            case AVAssetExportSessionStatusUnknown:
                                
                                NSLog(@"Export unknown %@", [[exportSession error] localizedDescription]);
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                                
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                
                                break;
                            case AVAssetExportSessionStatusCompleted:
                                NSLog(@"AVAssetExportSessionStatusCompleted");
                                
                                __block NSString *exportFileName = [self getMD5FileString:filePath  docPath:docsPath subtype:outputExtension];
                                [manager requestImageForAsset:asset targetSize:CGSizeMake(512, 512) contentMode:PHImageContentModeAspectFill options:requestOptions resultHandler:^(UIImage * _Nullable thumbnailResult, NSDictionary * _Nullable info) {
                                    @autoreleasepool {
                                        if(thumbnailResult != nil){
                                            NSString *thumbnailPath = [exportFileName stringByReplacingOccurrencesOfString:outputExtension withString:@"-thumbnail.jpg"];
                                            NSData * binaryImageData = UIImageJPEGRepresentation(thumbnailResult, self.quality/100.0f);
                                            NSError *err;
                                            if (![binaryImageData writeToFile:thumbnailPath options:NSAtomicWrite error:&err] ) {
                                                NSLog(@"Error: %@", err);
                                                internalIndex ++;
                                                nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                                    [self processAssets:fetchAssets
                                                                  index:internalIndex
                                                               docsPath:docsPath
                                                             targetSize:targetSize
                                                                manager:manager
                                                         requestOptions:requestOptions
                                                          startEndTimes:nil
                                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                                });
                                            } else {
                                                [manager requestImageForAsset:asset targetSize:CGSizeMake(asset.pixelWidth, asset.pixelHeight) contentMode:PHImageContentModeDefault options:requestOptions resultHandler:^(UIImage * _Nullable previewResult, NSDictionary * _Nullable info) {
                                                    @autoreleasepool {
                                                        if(previewResult != nil){
                                                            NSString *previewPath = [exportFileName stringByReplacingOccurrencesOfString:outputExtension withString:@"-preview.jpg"];
                                                            NSData * binaryImageData = UIImageJPEGRepresentation(previewResult, self.quality/100.0f);
                                                            NSError *err;
                                                            if (![binaryImageData writeToFile:previewPath options:NSAtomicWrite error:&err] ) {
                                                                NSLog(@"Error: %@", err);
                                                                internalIndex ++;
                                                                nextCallback(internalIndex,nil, nil, localIdentifier, nil);
                                                            } else {
                                                                internalIndex++;
                                                                nextCallback(internalIndex,[[NSURL fileURLWithPath:exportFileName] absoluteString], localIdentifier, nil, metaDic);
                                                            }
                                                            dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                                                [self processAssets:fetchAssets
                                                                              index:internalIndex
                                                                           docsPath:docsPath
                                                                         targetSize:targetSize
                                                                            manager:manager
                                                                     requestOptions:requestOptions
                                                                      startEndTimes:nil
                                                                  completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                                            });
                                                        }
                                                    }
                                                }];
                                            }
                                        }
                                    }
                                }];
                                
                                
                                
                                break;
                        }
                        
                    }];
                     
                    
                    /*BOOL maintainAspectRatio = YES;
                    float videoWidth = mediaSize.width;
                    float videoHeight = mediaSize.height;
                    int newWidth;
                    int newHeight;
                    
                    if (maintainAspectRatio) {
                        float aspectRatio = videoWidth / videoHeight;
                        
                        // for some portrait videos ios gives the wrong width and height, this fixes that
                        NSString *videoOrientation = [self getOrientationForTrack:avasset];
                        if ([videoOrientation isEqual: @"portrait"]) {
                            if (videoWidth > videoHeight) {
                                videoWidth = mediaSize.height;
                                videoHeight = mediaSize.width;
                                aspectRatio = videoWidth / videoHeight;
                            }
                        }
                        
                        newWidth = (width && height) ? height * aspectRatio : videoWidth;
                        newHeight = (width && height) ? newWidth / aspectRatio : videoHeight;
                    } else {
                        newWidth = (width && height) ? width : videoWidth;
                        newHeight = (width && height) ? height : videoHeight;
                    }
                    NSString *stringOutputFileType = AVFileTypeMPEG4;
                    NSString *outputExtension = @".mp4";
                    
                    SDAVAssetExportSession *encoder = [[SDAVAssetExportSession alloc] initWithAsset:avasset];
                    encoder.outputFileType = stringOutputFileType;
                    encoder.outputURL = furl;
                    encoder.shouldOptimizeForNetworkUse = YES;
                    encoder.videoSettings = @
                    {
                    AVVideoCodecKey: AVVideoCodecH264,
                    AVVideoWidthKey: [NSNumber numberWithInt: newWidth],
                    AVVideoHeightKey: [NSNumber numberWithInt: newHeight],
                    AVVideoCompressionPropertiesKey: @
                        {
                        AVVideoAverageBitRateKey: @6000000,
                        AVVideoProfileLevelKey: AVVideoProfileLevelH264High40,
                        },
                    };
                    encoder.audioSettings = @
                    {
                    AVFormatIDKey: @(kAudioFormatMPEG4AAC),
                    AVNumberOfChannelsKey: @2,
                    AVSampleRateKey: @44100,
                    AVEncoderBitRateKey: @128000,
                    };
                    
//                    encoder.timeRange = range;
                    
                    dispatch_semaphore_t sessionWaitSemaphore = dispatch_semaphore_create(0);
                    
                    void (^completionHandler)(void) = ^(void)
                    {
                        dispatch_semaphore_signal(sessionWaitSemaphore);
                    };
                    
                    // do it
                    [self.commandDelegate runInBackground:^{
                        [encoder exportAsynchronouslyWithCompletionHandler:completionHandler];
                        do {
                            dispatch_time_t dispatchTime = DISPATCH_TIME_FOREVER;  // if we dont want progress, we will wait until it finishes.
                            dispatchTime = getDispatchTimeFromSeconds((float)1.0);
                            double progress = [encoder progress] * 100;
                            
                            progressCallback(internalIndex, progress);
                            dispatch_semaphore_wait(sessionWaitSemaphore, dispatchTime);
                        } while( [encoder status] < AVAssetExportSessionStatusCompleted );
                        
                        switch ([encoder status]) {
                            case AVAssetExportSessionStatusFailed:
                                
                                NSLog(@"Export failed: %@", [[encoder error] localizedDescription]);
                                
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier);
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                break;
                            case AVAssetExportSessionStatusCancelled:
                                
                                NSLog(@"Export canceled %@", [[encoder error] localizedDescription]);
                                
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier);
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                break;
                            case AVAssetExportSessionStatusWaiting:
                                NSLog(@"Export waiting");
                                break;
                            case AVAssetExportSessionStatusExporting:
                                NSLog(@"Export exporting");
                                break;
                            case AVAssetExportSessionStatusUnknown:
                                
                                NSLog(@"Export unknown %@", [[encoder error] localizedDescription]);
                                internalIndex ++;
                                nextCallback(internalIndex,nil, nil, localIdentifier);
                                
                            {
                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                    [self processAssets:fetchAssets
                                                  index:internalIndex
                                               docsPath:docsPath
                                             targetSize:targetSize
                                                manager:manager
                                         requestOptions:requestOptions
                                          startEndTimes:nil
                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                });
                            }
                                
                                break;
                            case AVAssetExportSessionStatusCompleted:
                                NSLog(@"AVAssetExportSessionStatusCompleted");
                                
                                __block NSString *exportFileName = [self getMD5FileString:filePath  docPath:docsPath subtype:outputExtension];
                                [manager requestImageForAsset:asset targetSize:CGSizeMake(512, 512) contentMode:PHImageContentModeDefault options:requestOptions resultHandler:^(UIImage * _Nullable result, NSDictionary * _Nullable info) {
                                    @autoreleasepool {
                                        if(result != nil){
                                            NSString *thumbnailPath = [exportFileName stringByReplacingOccurrencesOfString:outputExtension withString:@"-thumbnail.jpg"];
                                            NSData * binaryImageData = UIImageJPEGRepresentation(result, self.quality/100.0f);
                                            NSError *err;
                                            if (![binaryImageData writeToFile:thumbnailPath options:NSAtomicWrite error:&err] ) {
                                                NSLog(@"Error: %@", err);
                                                internalIndex ++;
                                                nextCallback(internalIndex,nil, nil, localIdentifier);
                                                dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                                    [self processAssets:fetchAssets
                                                                  index:internalIndex
                                                               docsPath:docsPath
                                                             targetSize:targetSize
                                                                manager:manager
                                                         requestOptions:requestOptions
                                                          startEndTimes:nil
                                                      completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                                });
                                            } else {
                                                [manager requestImageForAsset:asset targetSize:CGSizeMake(asset.pixelWidth, asset.pixelHeight) contentMode:PHImageContentModeDefault options:requestOptions resultHandler:^(UIImage * _Nullable result, NSDictionary * _Nullable info) {
                                                    @autoreleasepool {
                                                        if(result != nil){
                                                            NSString *previewPath = [exportFileName stringByReplacingOccurrencesOfString:outputExtension withString:@"-preview.jpg"];
                                                            NSData * binaryImageData = UIImageJPEGRepresentation(result, self.quality/100.0f);
                                                            NSError *err;
                                                            if (![binaryImageData writeToFile:previewPath options:NSAtomicWrite error:&err] ) {
                                                                NSLog(@"Error: %@", err);
                                                                internalIndex ++;
                                                                nextCallback(internalIndex,nil, nil, localIdentifier);
                                                            } else {
                                                                internalIndex++;
                                                                nextCallback(internalIndex,[[NSURL fileURLWithPath:exportFileName] absoluteString], localIdentifier, nil);
                                                            }
                                                            dispatch_group_async(dispatch_group_create(), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                                                                [self processAssets:fetchAssets
                                                                              index:internalIndex
                                                                           docsPath:docsPath
                                                                         targetSize:targetSize
                                                                            manager:manager
                                                                     requestOptions:requestOptions
                                                                      startEndTimes:nil
                                                                  completedCallback:completedCallback nextCallback:nextCallback progressCallback:progressCallback errorCallback:errorCallback];
                                                            });
                                                        }
                                                    }
                                                }];
                                            }
                                        }
                                    }
                                }];
                                
                                
                                
                                break;
                        }
                        
                        
                    }];*/
                }
            }];
        }
        
    }
    
}

-(NSString*) getMD5FileString:(NSString*)filePath docPath:(NSString*)docsPath subtype:(NSString*)subtype{
    NSString *executableFileMD5Hash = [FileHash md5HashOfFileAtPath:filePath];
    
    NSError * err = NULL;
    NSFileManager * fm = [[NSFileManager alloc] init];
    NSString *newFileName = [NSString stringWithFormat:@"%@/%@%@", docsPath, executableFileMD5Hash, subtype];
    BOOL result = [fm moveItemAtPath:filePath
                              toPath:newFileName
                               error:&err];
    if(!result){
        NSLog(@"Error: %@", err);
        return filePath;
    }
    return newFileName;
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
                                     toolBar.frame.size.height - margin*2 );
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
        NSLog(@"could not scale image");
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


- (void) transcodeVideo:(NSDictionary*)command
{
    NSDictionary* options = command;
    
    if ([options isKindOfClass:[NSNull class]]) {
        options = [NSDictionary dictionary];
    }
    
    NSString *inputFilePath = [options objectForKey:@"fileUri"];
    NSURL *inputFileURL = [self getURLFromFilePath:inputFilePath];
    NSString *videoFileName = [options objectForKey:@"outputFileName"];
    
    BOOL optimizeForNetworkUse = ([options objectForKey:@"optimizeForNetworkUse"]) ? [[options objectForKey:@"optimizeForNetworkUse"] intValue] : NO;
    
    //float videoDuration = [[options objectForKey:@"duration"] floatValue];
    BOOL maintainAspectRatio = [options objectForKey:@"maintainAspectRatio"] ? [[options objectForKey:@"maintainAspectRatio"] boolValue] : YES;
    float width = [[options objectForKey:@"width"] floatValue];
    float height = [[options objectForKey:@"height"] floatValue];
    int videoBitrate = ([options objectForKey:@"videoBitrate"]) ? [[options objectForKey:@"videoBitrate"] intValue] : 1000000; // default to 1 megabit
    int audioChannels = ([options objectForKey:@"audioChannels"]) ? [[options objectForKey:@"audioChannels"] intValue] : 2;
    int audioSampleRate = ([options objectForKey:@"audioSampleRate"]) ? [[options objectForKey:@"audioSampleRate"] intValue] : 44100;
    int audioBitrate = ([options objectForKey:@"audioBitrate"]) ? [[options objectForKey:@"audioBitrate"] intValue] : 128000; // default to 128 kilobits
    
    NSString *stringOutputFileType = Nil;
    NSString *outputExtension = Nil;
    
    //    switch (outputFileType) {
    //        case QUICK_TIME:
    //            stringOutputFileType = AVFileTypeQuickTimeMovie;
    //            outputExtension = @".mp4";
    //            break;
    //        case M4A:
    //            stringOutputFileType = AVFileTypeAppleM4A;
    //            outputExtension = @".m4a";
    //            break;
    //        case M4V:
    //            stringOutputFileType = AVFileTypeAppleM4V;
    //            outputExtension = @".m4v";
    //            break;
    //        case MPEG4:
    //        default:
    stringOutputFileType = AVFileTypeMPEG4;
    outputExtension = @".mp4";
    //            break;
    //    }
    // check if the video can be saved to photo album before going further
    
    AVURLAsset *avAsset = [AVURLAsset URLAssetWithURL:inputFileURL options:nil];
    
    NSString *cacheDir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *outputPath = [NSString stringWithFormat:@"%@/%@%@", cacheDir, videoFileName, outputExtension];
    NSURL *outputURL = [NSURL fileURLWithPath:outputPath];
    
    NSArray *tracks = [avAsset tracksWithMediaType:AVMediaTypeVideo];
    AVAssetTrack *track = [tracks objectAtIndex:0];
    CGSize mediaSize = track.naturalSize;
    
    float videoWidth = mediaSize.width;
    float videoHeight = mediaSize.height;
    int newWidth;
    int newHeight;
    
    if (maintainAspectRatio) {
        float aspectRatio = videoWidth / videoHeight;
        
        // for some portrait videos ios gives the wrong width and height, this fixes that
        NSString *videoOrientation = [self getOrientationForTrack:avAsset];
        if ([videoOrientation isEqual: @"portrait"]) {
            if (videoWidth > videoHeight) {
                videoWidth = mediaSize.height;
                videoHeight = mediaSize.width;
                aspectRatio = videoWidth / videoHeight;
            }
        }
        
        newWidth = (width && height) ? height * aspectRatio : videoWidth;
        newHeight = (width && height) ? newWidth / aspectRatio : videoHeight;
    } else {
        newWidth = (width && height) ? width : videoWidth;
        newHeight = (width && height) ? height : videoHeight;
    }
    
    NSLog(@"input videoWidth: %f", videoWidth);
    NSLog(@"input videoHeight: %f", videoHeight);
    NSLog(@"output newWidth: %d", newWidth);
    NSLog(@"output newHeight: %d", newHeight);
    
    SDAVAssetExportSession *encoder = [SDAVAssetExportSession.alloc initWithAsset:avAsset];
    encoder.outputFileType = stringOutputFileType;
    encoder.outputURL = outputURL;
    encoder.shouldOptimizeForNetworkUse = optimizeForNetworkUse;
    encoder.videoSettings = @
    {
    AVVideoCodecKey: AVVideoCodecH264,
    AVVideoWidthKey: [NSNumber numberWithInt: newWidth],
    AVVideoHeightKey: [NSNumber numberWithInt: newHeight],
    AVVideoCompressionPropertiesKey: @
        {
        AVVideoAverageBitRateKey: [NSNumber numberWithInt: videoBitrate],
        AVVideoProfileLevelKey: AVVideoProfileLevelH264High40
        }
    };
    encoder.audioSettings = @
    {
    AVFormatIDKey: @(kAudioFormatMPEG4AAC),
    AVNumberOfChannelsKey: [NSNumber numberWithInt: audioChannels],
    AVSampleRateKey: [NSNumber numberWithInt: audioSampleRate],
    AVEncoderBitRateKey: [NSNumber numberWithInt: audioBitrate]
    };
    
    /* // setting timeRange is not possible due to a bug with SDAVAssetExportSession (https://github.com/rs/SDAVAssetExportSession/issues/28)
     if (videoDuration) {
     int32_t preferredTimeScale = 600;
     CMTime startTime = CMTimeMakeWithSeconds(0, preferredTimeScale);
     CMTime stopTime = CMTimeMakeWithSeconds(videoDuration, preferredTimeScale);
     CMTimeRange exportTimeRange = CMTimeRangeFromTimeToTime(startTime, stopTime);
     encoder.timeRange = exportTimeRange;
     }
     */
    
    //  Set up a semaphore for the completion handler and progress timer
    dispatch_semaphore_t sessionWaitSemaphore = dispatch_semaphore_create(0);
    
    void (^completionHandler)(void) = ^(void)
    {
        dispatch_semaphore_signal(sessionWaitSemaphore);
    };
    
    // do it
    
    [self.commandDelegate runInBackground:^{
        [encoder exportAsynchronouslyWithCompletionHandler:completionHandler];
        
        do {
            dispatch_time_t dispatchTime = DISPATCH_TIME_FOREVER;  // if we dont want progress, we will wait until it finishes.
            dispatchTime = getDispatchTimeFromSeconds((float)1.0);
            double progress = [encoder progress] * 100;
            
            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];
            
            dispatch_semaphore_wait(sessionWaitSemaphore, dispatchTime);
        } while( [encoder status] < AVAssetExportSessionStatusCompleted );
        
        // this is kinda odd but must be done
        if ([encoder status] == AVAssetExportSessionStatusCompleted) {
            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            // AVAssetExportSessionStatusCompleted will not always mean progress is 100 so hard code it below
            double progress = 100.00;
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];
            
        }
        
        if (encoder.status == AVAssetExportSessionStatusCompleted)
        {
            NSLog(@"Video export succeeded");
            
        }
        else if (encoder.status == AVAssetExportSessionStatusCancelled)
        {
            NSLog(@"Video export cancelled");
            
        }
        else
        {
            NSString *error = [NSString stringWithFormat:@"Video export failed with error: %@ (%ld)", encoder.error.localizedDescription, (long)encoder.error.code];
            NSLog(@"%@",error);
        }
    }];
}
// inspired by http://stackoverflow.com/a/6046421/1673842
- (NSString*)getOrientationForTrack:(AVAsset *)asset
{
    AVAssetTrack *videoTrack = [[asset tracksWithMediaType:AVMediaTypeVideo] objectAtIndex:0];
    CGSize size = [videoTrack naturalSize];
    CGAffineTransform txf = [videoTrack preferredTransform];
    
    if (size.width == txf.tx && size.height == txf.ty)
        return @"landscape";
    else if (txf.tx == 0 && txf.ty == 0)
        return @"landscape";
    else if (txf.tx == 0 && txf.ty == size.width)
        return @"portrait";
    else
        return @"portrait";
}

- (NSURL*)getURLFromFilePath:(NSString*)filePath
{
    if ([filePath containsString:@"assets-library://"]) {
        return [NSURL URLWithString:[filePath stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    } else if ([filePath containsString:@"file://"]) {
        return [NSURL URLWithString:[filePath stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    }
    
    return [NSURL fileURLWithPath:[filePath stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
}

static dispatch_time_t getDispatchTimeFromSeconds(float seconds) {
    long long milliseconds = seconds * 1000.0;
    dispatch_time_t waitTime = dispatch_time( DISPATCH_TIME_NOW, 1000000LL * milliseconds );
    return waitTime;
}
@end

