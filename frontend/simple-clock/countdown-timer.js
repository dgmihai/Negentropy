import {AbstractTimer} from './abstract-timer.js';
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class CountdownTimer extends AbstractTimer {

  _formatTime(time) {
    var sign = time > 0 ? '+' : '';
    var absTime = Math.abs(time);
    var seconds = Math.round(absTime % 60);
    var minutes = Math.floor(absTime / 60) % 60;
    if (seconds == 60) {
      seconds = 0;
      minutes += 1;
    }
    var hours = Math.floor(absTime / 3600);
    return sign +
      (hours > 0 ? hours + 'h ' : '') +
      (minutes > 0 || hours > 0 ? minutes + 'm ' : '') +
      (seconds + 's');
  }
}

customElements.define('countdown-timer', CountdownTimer);
