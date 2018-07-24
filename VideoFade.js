import React, { Component } from 'react'
import { View } from 'react-native'
import Video from './Video'

export default class VideoFade extends Component {
    constructor(props) {
        super(props)

        this.state = {
            v1: {
                volume: undefined,
                snap: null,
                tag: null,
                queue: {}
            },
            v2: {
                volume: undefined,
                snap: null,
                tag: null,
                queue: {}
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
        if (this.props[name] && this.state[this.state.active].tag === e.target) {
            this.props[name](e);
        } else {
            if (name !== 'onProgress') {
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
        this[this.state.active].seek(time);
    }
    playLocal = (file, cb) => {
        this[this.state.active].playLocal(file, cb);
    }
    onLoadStart = (v, e) => {
        this.state[v].tag = e.target;

        this.parseEvent.call(this, e, 'onLoadStart')
    }
    onLoad = (v, e) => {
        this.parseEvent.call(this, e, 'onLoad')
    }
    onBuffer = (v, e) => {
        this.parseEvent.call(this, e, 'onBuffer')
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
            this.parseEvent.call(this, e, 'onEnd')
        }
    }
    onProgress = (v, e) => {
        if (v === this.state.active) {
            const timeLeft = e.playableDuration - e.currentTime;

            if (this.props.crossFade && !this.state.startedTrans && timeLeft <= this.state.crossFadeTime) {

                this.state[this.state.active].snap = this.getNativeProps.call(this, this.state.active);
                this.state.active = this.state.active === 'v1' ? 'v2' : 'v1';
                this.startFade.call(this);

                if (this.props.onEnd) {
                    this.props.onEnd();

                    if (this.props.onLoad) {
                        this.props.onLoad(this.state[this.state.active].queue.onLoad)
                    }
                }
            }
        }
    }
    onError = (e) => {
        this.parseEvent.call(this, e, 'onError')
    }
    onAudioBecomingNoisy = (e) => {
        this.parseEvent.call(this, e, 'onAudioBecomingNoisy')
    }
    onAudioFocusChanged = (e) => {
        this.parseEvent.call(this, e, 'onAudioFocusChanged')
    }
    onRemoteChange = (e) => {
        this.parseEvent.call(this, e, 'onRemoteChange')
    }
    //#endregion

    getNativeProps(v) {
        const { state, props } = this;

        if (!state.isDual && props.preload) {
            state.isDual = true;
        }

        if (this.state[v].snap) { return this.state[v].snap }

        return Object.assign({}, props, {
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
            paused: props.paused ? true : state.active === v ? false : true
        }, v === state.active ? props.source : { source: { uri: props.preload } });
    }

    render() {
        const state = this.state;
        const v1 = this.getNativeProps.call(this, 'v1');
        const v2 = state.isDual ? this.getNativeProps.call(this, 'v2') : null;

        return (
            <View>
                {v2 && <View style={{ opacity: state.active === 'v2' ? 1.0 : 0.0 }}>
                    <Video ref={comp => this.mountRef.call(this, comp, 'v2')} {...v2} />
                </View>}

                <Video ref={comp => this.mountRef.call(this, comp, 'v1')} {...v1} />
            </View>
        )
    }
}