//
//  ImageViewer.m
//  Helper
//
//  Created by Calvin Lai on 7/11/13.
//
//

#import "PhotoCaptionInputViewCordova.h"
// #import "MWPhotoBrowser.h"
#import <Cordova/CDVViewController.h>
// #import "MHOverviewController.h"
// #import <Cordova/CDVDebug.h>


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

- (void)showGallery:(CDVInvokedUrlCommand*)command {
    NSLog(@"showGalleryWith:%@", command.arguments);
    self.callbackId = command.callbackId;
    [self.callbackIds setValue:command.callbackId forKey:@"showGallery"];
    
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSMutableArray *images = [[NSMutableArray alloc] init];
    NSMutableArray *thumbs = [[NSMutableArray alloc] init];
    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    
    for (NSString* url in [options objectForKey:@"images"])
    {
        [images addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    for (NSString* url in [options objectForKey:@"thumbnails"])
    {
        [thumbs addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    
    self.photos = images;
    self.thumbnails = thumbs;
    
    PhotoCaptionInputViewController *vc = [[PhotoCaptionInputViewController alloc] initWithPhotos:images thumbnails:thumbs];
    vc.delegate = self;
    [self.viewController presentViewController:vc animated:YES completion:^{
        
    }];

    //[nc release];
    
    // Release
    //[browser release];
    //[images release];
    
}

- (void)showBrowser:(CDVInvokedUrlCommand*)command {
    NSLog(@"showBrowser :%@", command.arguments);
    self.callbackId = command.callbackId;
    [self.callbackIds setValue:command.callbackId forKey:@"showGallery"];
    
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSMutableArray *images = [[NSMutableArray alloc] init];
    NSMutableArray *thumbs = [[NSMutableArray alloc] init];
    NSUInteger photoIndex = [[options objectForKey:@"index"] intValue];
    
    for (NSString* url in [options objectForKey:@"images"])
    {
        [images addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    for (NSString* url in [options objectForKey:@"thumbnails"])
    {
        [thumbs addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    
    self.photos = images;
    self.thumbnails = thumbs;
}

#pragma mark - PhotoCaptionInputViewDelegate


// #pragma mark - MWPhotoBrowserDelegate

// - (NSUInteger)numberOfPhotosInPhotoBrowser:(MWPhotoBrowser *)photoBrowser {
//     return _photos.count;
// }

// - (MWPhoto *)photoBrowser:(MWPhotoBrowser *)photoBrowser photoAtIndex:(NSUInteger)index {
//     if (index < _photos.count)
//         return [_photos objectAtIndex:index];
//     return nil;
// }
// - (id <MWPhoto>)photoBrowser:(MWPhotoBrowser *)photoBrowser thumbPhotoAtIndex:(NSUInteger)index{
    
//     if([self.thumbnails count] > index ){
//        return  [self.thumbnails objectAtIndex:index];
//     }else{
//         return [self.photos objectAtIndex:index];
//     }
// }
// - (MWCaptionView *)photoBrowser:(MWPhotoBrowser *)photoBrowser captionViewForPhotoAtIndex:(NSUInteger)index {
//     MWPhoto *photo = [self.photos objectAtIndex:index];
//     MWCaptionView *captionView = [[MWCaptionView alloc] initWithPhoto:photo];
//     return captionView;
// }

// - (NSString *)photoBrowser:(MWPhotoBrowser *)photoBrowser titleForPhotoAtIndex:(NSUInteger)index{
//     return [NSString stringWithFormat:@"photo %lu",(unsigned long)index];
// }
// - (void)photoBrowser:(MWPhotoBrowser *)photoBrowser didDisplayPhotoAtIndex:(NSUInteger)index{
//     NSLog(@"didDisplayPhotoAtIndex %lu", (unsigned long) index );
// }
// - (void)photoBrowser:(MWPhotoBrowser *)photoBrowser actionButtonPressedForPhotoAtIndex:(NSUInteger)index{
//     NSLog(@"actionButtonPressedForPhotoAtIndex %lu", (unsigned long) index );
// }
// - (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser isPhotoSelectedAtIndex:(NSUInteger)index{
//     NSLog(@"isPhotoSelectedAtIndex %lu", (unsigned long) index);
//     return NO;
// }
// - (void)photoBrowser:(MWPhotoBrowser *)photoBrowser photoAtIndex:(NSUInteger)index selectedChanged:(BOOL)selected{
//     NSLog(@"photoAtIndex %lu selectedChanged %i", (unsigned long) index , selected);
//     [self.selected_photos setValue: @((selected ? 1 : 0)) forKey:[NSString stringWithFormat:@"%lu", (unsigned long)index] ];
    
// }
// - (void)photoBrowserDidFinishModalPresentation:(MWPhotoBrowser *)photoBrowser{
//     NSLog(@"photoBrowserDidFinishModalPresentation");
//     __block NSMutableArray *fileStrings = [[NSMutableArray alloc] init];
//     __block CDVPluginResult* result = nil;
    
//     [self.selected_photos enumerateKeysAndObjectsUsingBlock:^(NSString* key, id  _Nonnull obj, BOOL * _Nonnull stop) {
//         BOOL selected = (BOOL)obj;
//         if(selected){
//             MWPhoto * photo = [self.photos objectAtIndex:[key integerValue]];
//             [fileStrings addObject: [photo url] ];
//         }
//     }];
    
    
//     result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys: fileStrings, @"images", nil]];
//     [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
//     [self.viewController dismissViewControllerAnimated:YES completion:nil];
// }

@end
