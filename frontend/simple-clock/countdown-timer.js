import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class CountdownTimer extends PolymerElement {
  static get template() {
    return html`
      <style>
        :host {
          display: block;
        }
      </style>
      {{formattedTime}}
    `;
  }

  static get properties() {
    return {
      startTime: {
        type: Number,
        value: 0
      },
      currentTime: {
        type: Number,
        value: 0
      },
      isRunning: {
        type: Boolean,
        value: false
      },
      _elapsedTime: {
        type: Number,
        value: 0
      },
      formattedTime: {
        type: String,
        value: '0s'
      },
      _maxValue: {
        type: Number,
        value: 7 * 24 * 60 * 60
      }
    };
  }

  ready() {
    super.ready();
    this.set('currentTime', this.startTime);
    this.set('formattedTime', this._formatTime(this.currentTime));
  }

  play() {
    if (this.currentTime >= this._maxValue) return;
    this._elapsedTime = performance.now() / 1000;
    this.isRunning = true;
    window.requestAnimationFrame(this._updateTime.bind(this));
  }

  pause() {
    this.isRunning = false;
  }

  _updateTime(timestamp) {
    if (!this.isRunning) return;
    var now = timestamp / 1000;
    var progress = now - this._elapsedTime;
    this.currentTime = this.currentTime + progress;
    this.formattedTime = this._formatTime(this.currentTime);
    this._elapsedTime = now;
    window.requestAnimationFrame(this._updateTime.bind(this));
  }

  _formatTime(time) {
    var seconds = Math.abs(time) | 0;
    var minutes = (seconds / 60) | 0;
    var hours = (minutes / 60) | 0;
    minutes = hours > 0 ? minutes % 60 : minutes;
    seconds = seconds % 60;
    return (time >= 0 ? '+' : '') +
      (hours > 0 ? hours + 'h ' : '') +
      (minutes > 0 ? minutes + 'm ' : '') +
      (seconds + 's');
  }
}

customElements.define('countdown-timer', CountdownTimer);
