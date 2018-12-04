import React, { Component } from 'react'
import { View, Platform } from 'react-native'
import Video from './Video'

export default class VideoFade extends Component {
    constructor(props) {
        super(props)

        this.state = {
            v1: {
                volume: undefined,
                snap: null,
                tag: null,
                queue: {},
                duration: 0
            },
            v2: {
                volume: undefined,
                snap: null,
                tag: null,
                queue: {},
                duration: 0
            },
            active: 'v1',
            isDual: false,
            startedTrans: 0,
            crossFadeTime: 10 //secs
        }
    }
    mountRef = (comp, v) => {
        this[v] = comp;
    }
    parseEvent = (e, name) => {
        if (this.props[name] && (this.state[this.state.active].tag === e.tag || Platform.OS === 'android' && name === 'onRemoteChange')) { //android can only has one remotelistener
            this.props[name](e);
        } else {
            if (name !== 'onProgress' && name !== 'onRemoteChange') {
                this.state[this.state.active === 'v1' ? 'v2' : 'v1'].queue[name] = e
            }
        }
    }

    startFade = () => {
        let old = this.state.v1.snap ? 'v1' : 'v2';

        if (!this.state.startedTrans) {
            this.state.startedTrans = 1;
            this.state[old].volume = this.props.volume;
            this.state[this.state.active].volume = 0.0;
        } else {
            this.state.startedTrans += 1;
        }

        this.state[old].snap.volume -= 0.1;
        this.state[this.state.active].volume += 0.1;

        if (this.state.startedTrans >= 10) { //fade done
            this.state[old].snap = null;
            this.state.startedTrans = 0;
            this.state.v1.volume = undefined;
            this.state.v2.volume = undefined;
            this.setState(this.state);
        } else {
            this.setState(this.state);
            setTimeout(this.startFade.bind(this), 1000);
        }
    }

    //#region player actions
    seek = (time) => {
        let player = this[this.state.active];
        if (player && player.seek) { player.seek(time); }
    }
    playLocal = (file, cb) => {
        let v = this[this.state.active];
        if (v && v.playLocal) {
            v.playLocal(file, cb);
        }
    }
    onLoadStart = (v, e) => {
        this.state[v].tag = e.tag;

        this.parseEvent(e, 'onLoadStart')
    }
    onLoad = (v, e) => {
        this.state[v].duration = e.duration;
        this.parseEvent(e, 'onLoad')
    }
    onBuffer = (v, e) => {
        this.parseEvent(e, 'onBuffer')
    }
    onEnd = (v, e) => {
        if (!this.props.crossFade) {
            this.state.active = this.state.active === 'v1' ? 'v2' : 'v1';
            this.setState(this.state);

            if (this.props.onEnd) {
                this.props.onEnd();

                if (this.props.onLoad) {
                    this.props.onLoad(this.state[this.state.active].queue.onLoad)
                }
            }
        } else {
            this.parseEvent(e, 'onEnd')
        }
    }
    onProgress = (v, e) => {
        if (v === this.state.active && this.state[this.state.active].duration) {
            const timeLeft = this.state[this.state.active].duration - e.currentTime;

            if (this.props.crossFade && !this.state.startedTrans && timeLeft <= this.state.crossFadeTime) {

                this.state[this.state.active].snap = this.getNativeProps(this.state.active);
                this.state.active = this.state.active === 'v1' ? 'v2' : 'v1';
                this.startFade.call(this);

                if (this.props.onEnd) {
                    this.props.onEnd();

                    if (this.props.onLoad) {
                        let ol = this.state[this.state.active].queue.onLoad;

                        if (ol) { this.props.onLoad(ol) }
                    }
                }
            }
        }
    }
    onError = (v, e) => {
        this.parseEvent(e, 'onError')
    }
    onAudioBecomingNoisy = (v, e) => {
        this.parseEvent(e, 'onAudioBecomingNoisy')
    }
    onAudioFocusChanged = (v, e) => {
        this.parseEvent(e, 'onAudioFocusChanged')
    }
    onRemoteChange = (v, e) => {
        this.parseEvent(e, 'onRemoteChange')
    }
    //#endregion

    getNativeProps = (v) => {
        const { state, props } = this;

        if (!state.isDual && props.preload) {
            state.isDual = true;
        }

        if (this.state[v].snap) { return this.state[v].snap }

        let ret = Object.assign({}, props, {
            volume: this.state.startedTrans ? this.state[v].volume : this.props.volume,
            onLoad: this.onLoad.bind(this, v),
            onLoadStart: this.onLoadStart.bind(this, v),
            onBuffer: this.onBuffer.bind(this, v),
            onEnd: this.onEnd.bind(this, v),
            onProgress: this.onProgress.bind(this, v),
            onError: this.onError.bind(this, v),
            onAudioBecomingNoisy: this.onAudioBecomingNoisy.bind(this, v),
            onAudioFocusChanged: this.onAudioFocusChanged.bind(this, v),
            onRemoteChange: this.onRemoteChange.bind(this, v),
            preload: undefined,
            paused: props.paused ? true : state.active === v ? false : true,
        }, v === state.active ? {
            source: props.source,
            // metadata: props.metadata
        } : {
            source: { uri: props.preload },
            // metadata: undefined
        });

        return ret.source.uri ? ret : null;
    }

    shouldComponentUpdate(np, ns) {
        const fireEnd = () => {
            if (this.props.onLoad) {
                let ol = this.state[this.state.active].queue.onLoad;
                if (ol) { this.props.onLoad(ol) }
            }
        }

        if (this.props.source.uri !== np.source.uri) { //checking for next press, see if it matches the preload, if it does just switch active state then fire onLoad
            const v1 = this.getNativeProps('v1')
            const v2 = this.getNativeProps('v2')

            if (v1 && v1.source && np.source.uri === v1.source.uri) {
                this.state.active = 'v1'
                fireEnd();
            } else if (v2 && v2.source && np.source.uri === v2.source.uri) {
                this.state.active = 'v2'
                fireEnd();
            }
        }

        return true;
    }

    render() {
        const state = this.state;
        const v1 = this.getNativeProps('v1');
        const v2 = state.isDual ? this.getNativeProps('v2') : null;

        return (
            <View>
                <Video ref={comp => this.mountRef(comp, 'v1')} {...v1} />

                {v2 && <View style={{ position: 'absolute', opacity: state.active === 'v2' ? 1.0 : 0.0 }}>
                    <Video ref={comp => this.mountRef(comp, 'v2')} {...v2} />
                </View>}
            </View>
        )
    }
}