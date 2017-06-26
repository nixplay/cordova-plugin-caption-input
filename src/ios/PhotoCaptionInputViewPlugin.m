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
#import "MWPhotoExt.h"
#import "CustomViewController.h"
#define LIGHT_BLUE_COLOR [UIColor colorWithRed:(99/255.0f)  green:(176/255.0f)  blue:(228.0f/255.0f) alpha:1.0]
#define BUNDLE_UIIMAGE(imageNames) [UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), imageNames]]
#define BIN_UIIMAGE BUNDLE_UIIMAGE(@"images/bin.png")
#define SENDFRIEND_UIIMAGE BUNDLE_UIIMAGE(@"images/sendfriend.png")

@implementation PhotoCaptionInputViewPlugin

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
    
//    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    self.preSelectedAssets = [options objectForKey:@"preSelectedAssets"];
    
    NSDictionary * data = [options objectForKey:@"data"];
    NSArray *argfriends = [options objectForKey:@"friends"];
    photoDestination = PLAYLIST;
    if(![[argfriends class] isEqual:[NSNull class]]){
        self.friends = argfriends;
    }else{
        self.friends = [NSArray new];
        
    }
    NSLog(@"data %@",data);
    UIScreen *screen = [UIScreen mainScreen];
    CGFloat scale = screen.scale;
    // Sizing is very rough... more thought required in a real implementation
    CGFloat imageSize = MAX(screen.bounds.size.width, screen.bounds.size.height) * 1.5;
    CGSize imageTargetSize = CGSizeMake(imageSize * scale, imageSize * scale);
    CGSize thumbTargetSize = CGSizeMake(imageSize / 3.0 * scale, imageSize / 3.0 * scale);
    
    PHFetchResult<PHAsset *> * result = [PHAsset fetchAssetsWithLocalIdentifiers:self.preSelectedAssets options:nil];
    [result enumerateObjectsUsingBlock:^(PHAsset * _Nonnull asset, NSUInteger idx, BOOL * _Nonnull stop) {
        [images addObject:[MWPhotoExt photoWithAsset:asset targetSize:imageTargetSize] ];
        [thumbs addObject:[MWPhotoExt photoWithAsset:asset targetSize:thumbTargetSize] ];
    }];
    
    //    for (NSString* url in [options objectForKey:@"images"])
    //    {
    //        [images addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    //    }
    //    for (NSString* url in [options objectForKey:@"thumbnails"])
    //    {
    //        [thumbs addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    //    }
    
    self.photos = images;
    self.thumbnails = thumbs;
    
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:_photos thumbnails:_thumbnails preselectedAssets:self.preSelectedAssets delegate:self];
    _photoCaptionInputViewController = vc;
    CustomViewController *nc = [[CustomViewController alloc]initWithRootViewController:vc];
    //    [self.viewController presentViewController:nc animated:YES completion:^{
    //
    //    }];
    CATransition *transition = [[CATransition alloc] init];
    transition.duration = 0.35;
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromRight;
    [self.viewController.view.window.layer addAnimation:transition forKey:kCATransition];
    
    
    //    float initHeight = self.viewController.view.frame.size.height * (11.0f/12.0f);
    //    UIButton *button = [[UIButton alloc]initWithFrame:
    //                        CGRectMake(30,
    //                                   initHeight,
    //                                   self.viewController.view.frame.size.width-60,
    //                                   50)];
    //    //    [button setImage:[UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), @"images/send.png"]] forState:UIControlStateNormal];
    //    [button setBackgroundColor:LIGHT_BLUE_COLp…÷OR];
    //    button.layer.cornerRadius = 10; // this value vary as per your desire
    //    button.clipsToBounds = YES;
    //    [button setTitle:@"send" forState:UIControlStateNormal];
    //    button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin ;
    //    [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
    //    [nc.view addSubview:button];
    
    [self.viewController presentViewController:nc animated:NO completion:^{
    }];
    
    
}

-(void) onSendPressed:(id) sender{
    photoDestination = PLAYLIST;
    [_photoCaptionInputViewController getPhotosCaptions];
}
-(void) onFreindPressed:(id) sender{
    photoDestination = FRIENDS;
    [_photoCaptionInputViewController getPhotosCaptions];
}

#pragma mark - PhotoCaptionInputViewDelegate


-(void) dismissPhotoCaptionInputView:(PhotoCaptionInputViewController*)controller{
    CDVPluginResult* pluginResult = nil;
    NSString *message = [NSString stringWithFormat:@"No Result in PhotoCaptionInputViewPlugin (%@) ", NSLocalizedString(@"User canceled", nil)];
    NSLog(@"%@", message);
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT messageAsString:message];
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
            if(photoDestination == PLAYLIST){
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions", invalidImages, @"invalidImages", @"playlists", @"type", nil]];
            }else{
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions", invalidImages, @"invalidImages", @"friends", @"type", nil]];
            }
        }
    });
    
    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
        if (nil == result) {
            if(photoDestination == PLAYLIST){
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", @"playlists", @"type", nil]];
            }else{
                result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", @"friends", @"type", nil]];
            }
            
        }
        
        progressHUD.progress = 1.f;
        [progressHUD hide:YES];
        [self.viewController dismissViewControllerAnimated:YES completion:nil];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });
    
    
    
}

- (NSMutableArray*)photoBrowser:(MWPhotoBrowser *)photoBrowser buildToolbarItems:(UIToolbar*)toolBar{
    NSMutableArray *items = [[NSMutableArray alloc] init];
    
    UIBarButtonItem *flexSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:self action:nil];
    [items addObject:flexSpace];
    
//    UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
//    fixedSpace.width = 5;
//    [items addObject:fixedSpace];
    float margin = 2;
    
    
    
    
    [toolBar setBackgroundImage:[UIImage new]
                  forToolbarPosition:UIToolbarPositionAny
                          barMetrics:UIBarMetricsDefault];
    
    [toolBar setBackgroundColor:[UIColor clearColor]];
//    [items addObject:flexSpace];
    
//    if([self.friends count] == 0){
    BOOL hasFriend = YES;
    if(!hasFriend){
//        CGRect toolBarFrame = toolBar.frame;
//        CGRect viewControllerFrame = self.viewController.view.frame;
//        NSLog(@"toolBarFrame %@",NSStringFromCGRect(toolBarFrame));
//        NSLog(@"viewControllerFrame %@",NSStringFromCGRect(viewControllerFrame));
        CGRect newFrame = CGRectMake(0,0,
                                     self.viewController.view.frame.size.width - 10,
                                     toolBar.frame.size.height - margin*2 );
        UIButton *btn = [[UIButton alloc] initWithFrame: newFrame];
        [btn setBackgroundColor:LIGHT_BLUE_COLOR];
        btn.layer.cornerRadius = 5; // this value vary as per your desire
        btn.clipsToBounds = YES;
        [btn setTitle:NSLocalizedString(@"ADD_PHOTOS_TO_MY_PLAYLIST", nil) forState:UIControlStateNormal];
        [btn addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
//        btn.imageEdgeInsets = UIEdgeInsetsMake(10, 10, 10, 10);
//        btn.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
//        btn.contentEdgeInsets = UIEdgeInsetsMake(10, 10, 10, 10);
        UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:btn];
//
//        UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"ADD_PHOTOS_TO_MY_PLAYLIST", nil) style:UIBarButtonItemStylePlain target:self action:@selector(onSendPressed:)];
        [items addObject:addPhotoButton];
    }else{
        
        
        UIButton *button =  [UIButton buttonWithType:UIButtonTypeCustom];
        [button addTarget:self action:@selector(onFreindPressed:) forControlEvents:UIControlEventTouchUpInside];
        [button setImage:SENDFRIEND_UIIMAGE forState:UIControlStateNormal];
        
        [button setFrame:CGRectMake(0,0,
                                    (self.viewController.view.frame.size.width *.4),
                                    toolBar.frame.size.height )];
        
        
        NSMutableAttributedString *titleText = [[NSMutableAttributedString alloc] initWithString:NSLocalizedString(@"SEND_TO_FRIEND", nil)];
        UIFont * font = [UIFont systemFontOfSize:14] ;
        
        // Set the font to bold from the beginning of the string to the ","
        [titleText addAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                                  font , NSFontAttributeName ,
                                  [UIColor whiteColor] , NSForegroundColorAttributeName,
                                  nil]
                           range:NSMakeRange(0, titleText.length)];
        
        
        // Set the attributed string as the buttons' title text
        [button setAttributedTitle:titleText forState:UIControlStateNormal];
        
        CGSize imageSize = button.imageView.frame.size;
        CGSize titleSize = button.titleLabel.frame.size;
        
        CGFloat totalHeight = (imageSize.height + titleSize.height );
        
        button.imageEdgeInsets = UIEdgeInsetsMake(0,
                                                0.0f,
                                                0.0f,
                                                - titleSize.width);
        
        button.titleEdgeInsets = UIEdgeInsetsMake(0.0f,
                                                - imageSize.width,
                                                - totalHeight,
                                                0.0f);
        
        button.contentEdgeInsets = UIEdgeInsetsMake(0.0f,
                                                  0.0f,
                                                  titleSize.height,
                                                  0.0f);
        
//        UILabel *label = [[UILabel alloc]initWithFrame:CGRectMake(3, 5, 50, 20)];
//        
//        [label setText:NSLocalizedString(@"SEND_TO_FRIEND", nil)];
//        label.textAlignment = UITextAlignmentCenter;
//        [label setTextColor:[UIColor whiteColor]];
//        [label setBackgroundColor:[UIColor clearColor]];
//        [button addSubview:label];
        
//        UIButton *btn2 = [[UIButton alloc] initWithFrame: newFrame];
//        [btn2 setBackgroundColor:LIGHT_BLUE_COLOR];
//        btn2.layer.cornerRadius = 5; // this value vary as per your desire
//        btn2.clipsToBounds = YES;
//        [btn2 setTitle:NSLocalizedString(@"SEND_TO_FRIEND", nil) forState:UIControlStateNormal];
//        [btn2 addTarget:self action:@selector(onFreindPressed:) forControlEvents:UIControlEventTouchUpInside];
        UIBarButtonItem *addFriendsButton = [[UIBarButtonItem alloc] initWithCustomView:button];
        
        
//        UIImage *senFriend = SENDFRIEND_UIIMAGE;
//        UIBarButtonItem *addFriendsButton = [[UIBarButtonItem alloc] initWithImage:senFriend style:UIBarButtonItemStylePlain target:self action:@selector(onFreindPressed:)];
//        [addFriendsButton setTitle:NSLocalizedString(@"SEND_TO_FRIEND", nil)];
//        [addFriendsButton titlePositionAdjustmentForBarMetrics:UIBarMetricsDefault];
        [items addObject:addFriendsButton];
        
        UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
        fixedSpace.width = 10;
        [items addObject:fixedSpace];
        
        
        CGRect newFrame = CGRectMake(margin,0,
                                     (self.viewController.view.frame.size.width *.4) - 10,
                                     toolBar.frame.size.height - margin*2 );
        UIButton *btn = [[UIButton alloc] initWithFrame: newFrame];
        [btn setBackgroundColor:LIGHT_BLUE_COLOR];
        btn.layer.cornerRadius = 5; // this value vary as per your desire
        btn.clipsToBounds = YES;
        [btn setTitle:NSLocalizedString(@"ADD_TO_PLAYLIST", nil) forState:UIControlStateNormal];
        [btn addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
        btn.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
//        btn.imageEdgeInsets = UIEdgeInsetsMake(10, 10, 10, 10);
        UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:btn];
        [items addObject:addPhotoButton];
        
        
        
    }
//    [items addObject:fixedSpace];
    [items addObject:flexSpace];
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


@end
