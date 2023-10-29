import {AbstractTimer} from './abstract-timer.js';
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class CountdownTimer extends AbstractTimer {

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
    var sign = time > 0 ? '+' : '';
    var absTime = Math.abs(time);
    var seconds = Math.ceil(absTime) % 60;
    var minutes = Math.floor(absTime / 60) % 60;
    var hours = Math.floor(absTime / 3600);
    if (Math.floor(absTime) == 0) return '-';
    return sign +
      (hours > 0 ? hours + 'h ' : '') +
      (minutes > 0 ? minutes + 'm ' : '') +
      (seconds + 's');
  }
}

customElements.define('countdown-timer', CountdownTimer);
