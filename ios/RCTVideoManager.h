#import <React/RCTViewManager.h>

@import AVFoundation;

@interface RCTVideoManager : RCTViewManager

@property (strong, nonatomic) AVPlayerItem *activeAudio;
@property (strong, nonatomic) AVPlayer *activePlayer;

@property (strong, nonatomic) AVPlayerItem *localAudio;
@property (strong, nonatomic) AVPlayer *localPlayer;

@end
