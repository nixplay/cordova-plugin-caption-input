//
//  PhotoCaptionInputViewCordova.h
//  Helper
//
//  Created by James Kong on 21/04/2017.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "PhotoCaptionInputViewController.h"

@interface PhotoCaptionInputViewCordova : CDVPlugin <PhotoCaptionInputViewDelegate> {

    NSMutableDictionary* callbackIds;
    NSArray* photos;

}
@property (copy)   NSString* callbackId;
@property (nonatomic, retain) NSMutableDictionary* callbackIds;
@property (nonatomic, retain) NSArray *photos;
@property (nonatomic, retain) NSArray *thumbnails;
@property (nonatomic, retain) NSMutableDictionary *selected_photos;
@property (nonatomic, assign) NSArray *preSelectedAssets;
@property (nonatomic, assign) NSInteger width;
@property (nonatomic, assign) NSInteger height;
@property (nonatomic, assign) NSInteger quality;
@property (nonatomic, assign) NSInteger outputType;
@property (nonatomic,assign) PhotoCaptionInputViewController* photoCaptionInputViewController;
- (void)showCaptionInput:(CDVInvokedUrlCommand*)command ;

@end
