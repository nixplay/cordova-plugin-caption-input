//
//  ImageViewer.h
//  Helper
//
//  Created by Calvin Lai on 7/11/13.
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
- (void)showGallery:(CDVInvokedUrlCommand*)command;

@end
