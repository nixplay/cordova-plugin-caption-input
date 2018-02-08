//
//  PhotoCaptionInputViewPlugin.h
//  Helper
//
//  Created by James Kong on 21/04/2017.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "PhotoCaptionInputViewController.h"
#import "SDAVAssetExportSession.h"
@interface PhotoCaptionInputViewPlugin : CDVPlugin <PhotoCaptionInputViewDelegate> {

    NSMutableDictionary* callbackIds;

    NSArray* photos;
    
}
@property (copy)   NSString* callbackId;
@property (nonatomic, retain) NSMutableDictionary* callbackIds;
@property (nonatomic, retain) NSArray *photos;
@property (nonatomic, retain) NSArray *thumbnails;
@property (nonatomic, retain) NSArray *friends;
@property (nonatomic, retain) NSMutableDictionary *selected_photos;
@property (nonatomic, retain) NSMutableArray *buttonOptions;
@property (nonatomic, retain) NSString *destinationType;
@property (nonatomic, assign) NSArray *preSelectedAssets;
@property (nonatomic, assign) NSInteger width;
@property (nonatomic, assign) NSInteger height;
@property (nonatomic, assign) NSInteger quality;
@property (nonatomic, assign) NSInteger outputType;
@property (nonatomic, assign) NSInteger maximumImagesCount;
@property (nonatomic, assign) BOOL allow_video;
@property (nonatomic, assign) BOOL camera;
@property (nonatomic, assign) NSTimeInterval videoMaximumDuration;
@property (nonatomic, assign) AVAssetExportSessionStatus status;
@property (nonatomic, strong) SDAVAssetExportSession* exportSession;
@property (nonatomic,assign) PhotoCaptionInputViewController* photoCaptionInputViewController;
- (void)showCaptionInput:(CDVInvokedUrlCommand*)command ;

@end
