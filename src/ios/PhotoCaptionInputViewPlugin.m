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
    NSLog(@"%@", message);
    
    
     
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[NSDictionary new]];
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
    progressHUD.labelText = NSLocalizedString(@"PROCESSING",nil);
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
            
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions", invalidImages, @"invalidImages", _distinationType, KEY_TYPE, nil]];
            
        }
    });
    
    dispatch_group_notify(dispatchGroup, dispatch_get_main_queue(), ^{
        if (nil == result) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: preSelectedAssets, @"preSelectedAssets", fileStrings, @"images", captions, @"captions",  invalidImages, @"invalidImages", _distinationType, KEY_TYPE, nil]];
            
            
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
//    fixedSpace.width = -15;
//    [items addObject:fixedSpace];
    float margin = 1.0f;
    
    
    
    
    [toolBar setBackgroundImage:[UIImage new]
             forToolbarPosition:UIToolbarPositionAny
                     barMetrics:UIBarMetricsDefault];
    
    [toolBar setBackgroundColor:[UIColor blackColor]];
    
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
            NSString *labelText = [obj valueForKey:KEY_LABEL];
            if (idx ==0 ) {
                UIButton *button =  [UIButton buttonWithType:UIButtonTypeCustom];
                [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
                //                [button setImage:SENDFRIEND_UIIMAGE forState:UIControlStateNormal];
                
                CGRect newFrame = CGRectMake(0,0,
                                             (self.viewController.view.frame.size.width *.5)-6,
                                             toolBar.frame.size.height - margin*2 );
                [button setFrame:newFrame];
                [button setBackgroundColor:LIGHT_BLUE_COLOR];
                button.layer.cornerRadius = 2; // this value vary as per your desire
                button.clipsToBounds = YES;
                //                [button setTitle:labelText forState:UIControlStateNormal];
                [button setAttributedTitle:[self attributedString:labelText WithSize:TEXT_SIZE color:[UIColor whiteColor]] forState:UIControlStateNormal];
                button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
                //                NSMutableAttributedString *titleText = [[NSMutableAttributedString alloc] initWithString:labelText];
                //                UIFont * font = [UIFont systemFontOfSize:14] ;
                //
                //                // Set the font to bold from the beginning of the string to the ","
                //                [titleText addAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                //                                          font , NSFontAttributeName ,
                //                                          [UIColor whiteColor] , NSForegroundColorAttributeName,
                //                                          nil]
                //                                   range:NSMakeRange(0, titleText.length)];
                //
                //
                //                // Set the attributed string as the buttons' title text
                //                [button setAttributedTitle:titleText forState:UIControlStateNormal];
                //
                //                CGSize imageSize = button.imageView.frame.size;
                //                CGSize titleSize = button.titleLabel.frame.size;
                //
                //                CGFloat totalHeight = (imageSize.height + titleSize.height );
                //
                //                button.imageEdgeInsets = UIEdgeInsetsMake(0,
                //                                                          0.0f,
                //                                                          0.0f,
                //                                                          - titleSize.width);
                //
                //                button.titleEdgeInsets = UIEdgeInsetsMake(0.0f,
                //                                                          - imageSize.width,
                //                                                          - totalHeight,
                //                                                          0.0f);
                //
                //                button.contentEdgeInsets = UIEdgeInsetsMake(0.0f,
                //                                                            0.0f,
                //                                                            titleSize.height,
                //                                                            0.0f);
                UIBarButtonItem *addFriendsButton = [[UIBarButtonItem alloc] initWithCustomView:button];
                [items addObject:addFriendsButton];
                
                
            }else{
                
                CGRect newFrame = CGRectMake(0,0,
                                             (self.viewController.view.frame.size.width *.5)-6,
                                             toolBar.frame.size.height - margin*2 );
                UIButton *button = [[UIButton alloc] initWithFrame: newFrame];
                [button setBackgroundColor:LIGHT_BLUE_COLOR];
                button.layer.cornerRadius = 2; // this value vary as per your desire
                button.clipsToBounds = YES;
                //                [button setTitle:labelText forState:UIControlStateNormal];
                [button setAttributedTitle:[self attributedString:labelText WithSize:TEXT_SIZE color:[UIColor whiteColor]] forState:UIControlStateNormal];
                [button addTarget:self action:@selector(onSendPressed:) forControlEvents:UIControlEventTouchUpInside];
                button.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
                UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:button];
                [items addObject:addPhotoButton];
                
            }
            if(idx != [_buttonOptions count]-1){
                UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
                fixedSpace.width = -8;
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


@end
