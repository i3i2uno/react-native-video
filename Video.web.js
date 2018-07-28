import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { StyleSheet, View, Image } from 'react-native'

const styles = StyleSheet.create({
    base: {
        overflow: 'hidden',
    },
});

class RCTVideo extends Component {
    constructor(props) {
        super(props);

        this.state = {
            vid: null,
            isMounted: true
        }
    }
    componentWillUnmount() {
        this.state.isMounted = false;
    }
    connectVideoToActions(comp) {
        const p = this.props;
        const state = this.state;

        if (comp) {
            state.vid = comp;

            state.vid.oncanplay = () => {
                p.onVideoLoad({ nativeEvent: { duration: state.vid.duration } })
            }
            state.vid.onloadstart = () => {
                p.onVideoLoadStart();
            }
            state.vid.onended = () => {
                p.onVideoEnd({ nativeEvent: {} });
            }
            state.vid.onerror = (err) => {
                p.onVideoError({ nativeEvent: { error: err } });
            }
            this.startMonitoringProgress.call(this);
        }
    }
    startMonitoringProgress() {
        if (this.props.onProgress) {
            const startVideoChecking = () => {
                if (this.state.isMounted && !this.props.paused) {
                    this.props.onVideoProgress({
                        nativeEvent: {
                            currentTime: this.state.vid.currentTime,
                            seekableDuration: this.state.vid.duration
                        }
                    })
                    setTimeout(startVideoChecking.bind(this), 1000);
                }
            }
            startVideoChecking();
        }
    }
    componentWillReceiveProps(np) {
        if (this.state.vid && this.props.paused !== np.paused) {
            if (np.paused) {
                this.state.vid.pause();
            } else {
                this.state.vid.play();
                setTimeout(this.startMonitoringProgress.bind(this), 50);
            }
        }

        if (this.state.vid && this.props.volume !== np.volume) {
            this.state.vid.volume = np.volume;
        }

        if (this.state.vid && this.props.preload !== np.preload && np.preload) {
            var vid = document.createElement('video');

            vid.src = np.preload;
            vid.autoPlay = false;
        }
    }
    render() {
        const {
            src,
            style,
            resizeMode
        } = this.props;

        return (
            <video
                src={src.uri}
                style={StyleSheet.flatten([style, { objectFit: resizeMode }])}
                autoPlay={this.props.paused ? false : true}
                ref={(ref) => { if (!this.state.vid) { this.connectVideoToActions.bind(this)(ref); this.props._ref(ref); } }}
            ></video>
        )
    }
}

function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
    }

    return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
}

export default class RNVideo extends Component {

    constructor(props) {
        super(props);

        this.state = {
            showPoster: true,
            tag: guid()
        };
    }

    setNativeProps(nativeProps) {
        if (this._root && this._root.setNativeProps) {
            this._root.setNativeProps(nativeProps);
        }
    }

    seek = (time) => {
        this._root.currentTime = time;
    };

    directPlay = () => {
        this._root.play();
    };

    //CUSTOM
    _onRemoteChange = (event) => {
        if (this.props.onRemoteChange) {
            this.props.onRemoteChange(event.nativeEvent);
        }
    };

    playLocal = (file, cb) => {

    };
    //CUSTOM END

    presentFullscreenPlayer = () => {
        this.setNativeProps({ fullscreen: true });
    };

    dismissFullscreenPlayer = () => {
        this.setNativeProps({ fullscreen: false });
    };

    _assignRoot = (component) => {
        this._root = component;
    };

    _onLoadStart = (event) => {
        if (this.props.onLoadStart) {
            this.props.onLoadStart({ tag: this.state.tag });
        }
    };

    _onLoad = (event) => {
        if (this.props.onLoad) {
            this.props.onLoad(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };

    _onError = (event) => {
        if (this.props.onError) {
            this.props.onError(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };

    _onProgress = (event) => {
        if (this.props.onProgress) {
            this.props.onProgress(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };

    _onSeek = (event) => {
        if (this.state.showPoster) {
            this.setState({ showPoster: false });
        }

        if (this.props.onSeek) {
            this.props.onSeek(event.nativeEvent);
        }
    };

    _onEnd = (event) => {
        if (this.props.onEnd) {
            this.props.onEnd(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };

    _onTimedMetadata = (event) => {
        if (this.props.onTimedMetadata) {
            this.props.onTimedMetadata(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };

    _onFullscreenPlayerWillPresent = (event) => {
        if (this.props.onFullscreenPlayerWillPresent) {
            this.props.onFullscreenPlayerWillPresent(event.nativeEvent);
        }
    };

    _onFullscreenPlayerDidPresent = (event) => {
        if (this.props.onFullscreenPlayerDidPresent) {
            this.props.onFullscreenPlayerDidPresent(event.nativeEvent);
        }
    };

    _onFullscreenPlayerWillDismiss = (event) => {
        if (this.props.onFullscreenPlayerWillDismiss) {
            this.props.onFullscreenPlayerWillDismiss(event.nativeEvent);
        }
    };

    _onFullscreenPlayerDidDismiss = (event) => {
        if (this.props.onFullscreenPlayerDidDismiss) {
            this.props.onFullscreenPlayerDidDismiss(event.nativeEvent);
        }
    };

    _onReadyForDisplay = (event) => {
        if (this.props.onReadyForDisplay) {
            this.props.onReadyForDisplay(event.nativeEvent);
        }
    };

    _onPlaybackStalled = (event) => {
        if (this.props.onPlaybackStalled) {
            this.props.onPlaybackStalled(event.nativeEvent);
        }
    };

    _onPlaybackResume = (event) => {
        if (this.props.onPlaybackResume) {
            this.props.onPlaybackResume(event.nativeEvent);
        }
    };

    _onPlaybackRateChange = (event) => {
        if (this.state.showPoster && (event.nativeEvent.playbackRate !== 0)) {
            this.setState({ showPoster: false });
        }

        if (this.props.onPlaybackRateChange) {
            this.props.onPlaybackRateChange(event.nativeEvent);
        }
    };

    _onAudioBecomingNoisy = () => {
        if (this.props.onAudioBecomingNoisy) {
            this.props.onAudioBecomingNoisy();
        }
    };

    _onAudioFocusChanged = (event) => {
        if (this.props.onAudioFocusChanged) {
            this.props.onAudioFocusChanged(event.nativeEvent);
        }
    };

    _onBuffer = (event) => {
        if (this.props.onBuffer) {
            this.props.onBuffer(Object.assign(event.nativeEvent, { tag: this.state.tag }));
        }
    };
    render() {
        const resizeMode = this.props.resizeMode;
        const source = this.props.source || {};//resolveAssetSource(this.props.source) || {};

        let uri = source.uri || '';
        if (uri && uri.match(/^\//)) {
            uri = `file://${uri}`;
        }

        const isNetwork = !!(uri && uri.match(/^https?:/));
        const isAsset = !!(uri && uri.match(/^(assets-library|file|content|ms-appx|ms-appdata):/));

        let nativeResizeMode = resizeMode;
        // if (resizeMode === VideoResizeMode.stretch) {
        //   nativeResizeMode = NativeModules.UIManager.RCTVideo.Constants.ScaleToFill;
        // } else if (resizeMode === VideoResizeMode.contain) {
        //   nativeResizeMode = NativeModules.UIManager.RCTVideo.Constants.ScaleAspectFit;
        // } else if (resizeMode === VideoResizeMode.cover) {
        //   nativeResizeMode = NativeModules.UIManager.RCTVideo.Constants.ScaleAspectFill;
        // } else {
        //   nativeResizeMode = NativeModules.UIManager.RCTVideo.Constants.ScaleNone;
        // }

        const nativeProps = Object.assign({}, this.props);
        Object.assign(nativeProps, {
            style: [styles.base, nativeProps.style],
            resizeMode: nativeResizeMode,
            src: {
                uri,
                isNetwork,
                isAsset,
                type: source.type || '',
                mainVer: source.mainVer || 0,
                patchVer: source.patchVer || 0,
            },
            onVideoLoadStart: this._onLoadStart,
            onVideoLoad: this._onLoad,
            onVideoError: this._onError,
            onVideoProgress: this._onProgress,
            onVideoSeek: this._onSeek,
            onVideoEnd: this._onEnd,
            onVideoBuffer: this._onBuffer,
            onTimedMetadata: this._onTimedMetadata,
            onVideoFullscreenPlayerWillPresent: this._onFullscreenPlayerWillPresent,
            onVideoFullscreenPlayerDidPresent: this._onFullscreenPlayerDidPresent,
            onVideoFullscreenPlayerWillDismiss: this._onFullscreenPlayerWillDismiss,
            onVideoFullscreenPlayerDidDismiss: this._onFullscreenPlayerDidDismiss,
            onReadyForDisplay: this._onReadyForDisplay,
            onPlaybackStalled: this._onPlaybackStalled,
            onPlaybackResume: this._onPlaybackResume,
            onPlaybackRateChange: this._onPlaybackRateChange,
            onAudioFocusChanged: this._onAudioFocusChanged,
            onAudioBecomingNoisy: this._onAudioBecomingNoisy,

            //CUSTOM
            onRemoteChange: this._onRemoteChange
            //CUSTOM END
        });

        if (this.props.poster && this.props.showPoster) {
            const posterStyle = {
                position: 'absolute',
                left: 0,
                top: 0,
                right: 0,
                bottom: 0,
                resizeMode: 'contain',
            };

            return (
                <View style={nativeProps.style}>
                    <RCTVideo
                        _ref={this._assignRoot}
                        {...nativeProps}
                    />
                    <Image
                        style={posterStyle}
                        source={{ uri: this.props.poster }}
                    />
                </View>
            );
        }

        return (
            <RCTVideo
                _ref={this._assignRoot}
                {...nativeProps}
            />
        );
    }
}

RNVideo.propTypes = {
    //CUSTOM
    metadata: PropTypes.object,
    preload: PropTypes.string,

    onRemoteChange: PropTypes.func,
    playLocal: PropTypes.func,
    setTimeout: PropTypes.func,
    //CUSTOM END

    /* Native only */
    src: PropTypes.object,
    seek: PropTypes.number,
    fullscreen: PropTypes.bool,
    onVideoLoadStart: PropTypes.func,
    onVideoLoad: PropTypes.func,
    onVideoBuffer: PropTypes.func,
    onVideoError: PropTypes.func,
    onVideoProgress: PropTypes.func,
    onVideoSeek: PropTypes.func,
    onVideoEnd: PropTypes.func,
    onTimedMetadata: PropTypes.func,
    onVideoFullscreenPlayerWillPresent: PropTypes.func,
    onVideoFullscreenPlayerDidPresent: PropTypes.func,
    onVideoFullscreenPlayerWillDismiss: PropTypes.func,
    onVideoFullscreenPlayerDidDismiss: PropTypes.func,

    /* Wrapper component */
    source: PropTypes.oneOfType([
        PropTypes.shape({
            uri: PropTypes.string
        }),
        // Opaque type returned by require('./video.mp4')
        PropTypes.number
    ]),
    resizeMode: PropTypes.string,
    poster: PropTypes.string,
    repeat: PropTypes.bool,
    paused: PropTypes.bool,
    muted: PropTypes.bool,
    volume: PropTypes.number,
    rate: PropTypes.number,
    playInBackground: PropTypes.bool,
    playWhenInactive: PropTypes.bool,
    ignoreSilentSwitch: PropTypes.oneOf(['ignore', 'obey']),
    disableFocus: PropTypes.bool,
    controls: PropTypes.bool,
    currentTime: PropTypes.number,
    progressUpdateInterval: PropTypes.number,
    onLoadStart: PropTypes.func,
    onLoad: PropTypes.func,
    onBuffer: PropTypes.func,
    onError: PropTypes.func,
    onProgress: PropTypes.func,
    onSeek: PropTypes.func,
    onEnd: PropTypes.func,
    onFullscreenPlayerWillPresent: PropTypes.func,
    onFullscreenPlayerDidPresent: PropTypes.func,
    onFullscreenPlayerWillDismiss: PropTypes.func,
    onFullscreenPlayerDidDismiss: PropTypes.func,
    onReadyForDisplay: PropTypes.func,
    onPlaybackStalled: PropTypes.func,
    onPlaybackResume: PropTypes.func,
    onPlaybackRateChange: PropTypes.func,
    onAudioFocusChanged: PropTypes.func,
    onAudioBecomingNoisy: PropTypes.func,

    /* Required by react-native */
    scaleX: PropTypes.number,
    scaleY: PropTypes.number,
    translateX: PropTypes.number,
    translateY: PropTypes.number,
    rotation: PropTypes.number,
    ...View.propTypes,
};