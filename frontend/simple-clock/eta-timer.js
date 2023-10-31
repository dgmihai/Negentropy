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
      netDuration: {
        type: Number,
        value: 0
      },
      isActive: {
        type: Boolean,
        value: false
      },
      formattedTime: {
        type: String,
        value: '-'
      },
      _lastFrameTime: {
        type: Number,
        value: 0
      },
      showSeconds: {
        type: Boolean,
        value: false
      }
    };
  }

  ready() {
    super.ready();
    this._lastFrameTime = Date.now();
    this._tick = this._tick.bind(this);
    window.requestAnimationFrame(this._tick);
  }

  _calculateTime() {
    const currentTime = Date.now();

    if (this.isActive) {
      const elapsedTime = currentTime - this._lastFrameTime;
      this._lastFrameTime = currentTime;
      this.netDuration -= elapsedTime;
      if (this.netDuration < 0) this.netDuration = 0;
    }

    let time = currentTime + this.netDuration;
    const date = new Date(0);
    date.setMilliseconds(time);
    this.formattedTime = this._formatTime(date);
  }

  _tick() {
    this._calculateTime();
    window.requestAnimationFrame(this._tick);
  }

  _formatTime(time, timeZone = 'default') {
    // Hours are off by one for some reason?
    time.setHours(time.getHours() + 1);

    const options = {
      hour: 'numeric',
      minute: '2-digit',
      second: this.showSeconds ? '2-digit' : undefined,
      timeZone: timeZone !== 'default' ? timeZone : undefined,
    };

    const formatter = new Intl.DateTimeFormat('en-US', options);
    const parts = formatter.formatToParts(time);
    const formattedParts = parts.map(({ type, value }) => {
      if (type === 'dayPeriod') {
        return value.toUpperCase();
      }
      return value;
    });

    return formattedParts.join('');
  }
}

customElements.define('eta-timer', ETATimer);

