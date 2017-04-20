//
//  ImageViewer.m
//  Helper
//
//  Created by Calvin Lai on 7/11/13.
//
//

#import "PhotoCaptionInputViewCordova.h"
#import <Cordova/CDVViewController.h>

#import "MWPhotoExt.h"
@implementation PhotoCaptionInputViewCordova

@synthesize callbackId;
@synthesize callbackIds = _callbackIds;
@synthesize photos = _photos;
@synthesize thumbnails = _thumbnails;
- (NSMutableDictionary*)callbackIds {
    if(_callbackIds == nil) {
        _callbackIds = [[NSMutableDictionary alloc] init];
        self.selected_photos = [[NSMutableDictionary alloc] init];
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
    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    
    for (NSString* url in [options objectForKey:@"images"])
    {
        [images addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    }
    for (NSString* url in [options objectForKey:@"thumbnails"])
    {
        [thumbs addObject:[MWPhotoExt photoWithURL:[NSURL URLWithString: url]]];
    }
    
    self.photos = images;
    self.thumbnails = thumbs;
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:_photos thumbnails:_thumbnails delegate:self];
    UINavigationController *nc = [[UINavigationController alloc]initWithRootViewController:vc];
    nc.modalPresentationStyle = UIModalPresentationPopover;
    [self.viewController presentViewController:nc animated:YES completion:^{
        
    }];

    
}


#pragma mark - PhotoCaptionInputViewDelegate


-(void) onDismiss{
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
}
-(void) photoCaptionInputViewCaptions:(NSArray *)captions photos:(NSArray*)photos{
    __block CDVPluginResult* result = nil;
    result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: photos, @"images", captions, @"captions", nil]];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}
@end
