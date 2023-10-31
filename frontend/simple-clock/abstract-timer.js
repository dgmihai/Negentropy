import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

export class AbstractTimer extends PolymerElement {

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
        isActive: {
          type: Boolean,
          value: false
        },
        _elapsedTime: {
          type: Number,
          value: 0
        },
        formattedTime: {
          type: String,
          value: '-'
        },
        _maxValue: {
          type: Number,
          value: 7 * 24 * 60 * 60
        }
      };
    }

    ready() {
      super.ready();
      this.currentTime = this.startTime;
      this.formattedTime = this._formatTime(this.currentTime);
      this._updateTime = this._updateTime.bind(this);
    }

    play() {
      if (this.isRunning) return;
      if (this.currentTime >= this._maxValue) return;
      this._elapsedTime = performance.now() / 1000;
      this.isRunning = true;
      this.isActive = true;
      window.requestAnimationFrame(this._updateTime);
    }

    pause() {
      this.isRunning = false;
      this.isActive = false;
    }

    _updateTime(timestamp) {
      if (!this.isRunning) return;
      if (!this.currentTime >= this._maxValue) {
        this.pause();
        return;

      }
      var now = timestamp / 1000;

      var progress = now - this._elapsedTime;
      this.currentTime = this.currentTime + progress;

      this.formattedTime = this._formatTime(this.currentTime);

      this._elapsedTime = now;
      window.requestAnimationFrame(this._updateTime);
    }

    _formatTime(time) {
       return time.toString();
    }
}