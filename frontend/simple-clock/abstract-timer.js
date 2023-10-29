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
}