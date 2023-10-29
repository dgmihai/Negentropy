import {AbstractTimer} from './abstract-timer.js';
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class ETATimer extends AbstractTimer {

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

