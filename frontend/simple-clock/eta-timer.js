import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class ETATimer extends PolymerElement {
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
        value: '12:00 AM'
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
    this.formattedTime = this._calculateETA(this.currentTime);
    this._elapsedTime = now;
    window.requestAnimationFrame(this._updateTime.bind(this));
  }

  _calculateETA(time) {
    let currentDateTime = new Date();
    let futureDateTime = new Date(currentDateTime.getTime() + time * 1000);

    if (futureDateTime < currentDateTime) {
      futureDateTime.setDate(futureDateTime.getDate() + 1);
    }

    let hours = futureDateTime.getHours();
    let minutes = futureDateTime.getMinutes();
    let period = hours >= 12 ? 'PM' : 'AM';

    hours = hours % 12 || 12;
    minutes = minutes < 10 ? '0' + minutes : minutes;

    return `${hours}:${minutes} ${period}`;
  }
}

customElements.define('eta-timer', ETATimer);

