/**
@license MIT

Copyright (c) 2016 Anson Chung

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
//import '@polymer/polymer/polymer-legacy.js';

import {Polymer} from '@polymer/polymer/lib/legacy/polymer-fn.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

Polymer({
	  _template: html`
		<style>
		  :host {
			display: block;
		  }
		</style>
		{{_formattedTime}}
	  `,

      is: 'simple-clock',

      properties: {
        /**
        * Current time of the timer, in seconds
        */
        startTime: {
          type: Number,
          value: 0,
          notify: true
        },
        /**
        * Current time of the timer, in seconds
        */
        currentTime: {
          type: Number,
          value: 0,
          notify: true
        },
        /**
        * True if the timer is currently running
        * @default false
        */
        isRunning: {
          type: Boolean,
          reflectToAttribute: true,
          notify: true,
          value: false
        },
        /**
        * Time the timer has spent running since last progressed
        */
        _elapsedTime: {
          type: Number,
          value: 0
        },
        /**
        * If the output should be formatted in ETA format or not
        */
        showETA: {
          type: Boolean,
          notify: true,
          value: false
        },

        _formattedTime: {
          type: String,
          value: '0'
        },
        
        /**
        * Maximum time the time will display
        */
        _maxValue: {
          type: Number,
          value: 7 * 24 * 60 * 60
        }
      },

      ready: function() {
        this.set('currentTime', this.startTime);
        if (this.showETA) {
          this.set('_formattedTime', this._calculateETA(this.currentTime));
        } else {
          this.set('_formattedTime', this._formatTime(this.currentTime));
        }
      },

      play: function() {
        if (!this.elapsedTime >= this._maxValue) {
          // timer is at max
          return;
        }

        this._elapsedTime = performance.now()/1000;
        this.isRunning = true;
        window.requestAnimationFrame(this._doTimer.bind(this));
      },

      pause: function() {
        this.isRunning = false;
      },

      _doTimer: function(timestamp) {
        if (!this.isRunning) {
          return;
        }
        if (!this.currentTime >= this._maxValue) {
          // timer is at max
          this.pause();
          return;
        }

        var now = timestamp/1000;

        var progress = now - this._elapsedTime;
        this.currentTime = this.currentTime + progress;

        // Check the showETA flag
        if (this.showETA) {
          this._formattedTime = this._calculateETA(this.currentTime);
        } else {
          this._formattedTime = this._formatTime(this.currentTime);
        }

        this._elapsedTime = now;
        window.requestAnimationFrame(this._doTimer.bind(this));
      },

      _formatTime: function(time) {
        var timeString = Math.abs(time).toString().split('.');

        var seconds = timeString[0];
        var minutes = seconds / 60 | 0;
        var hours = minutes / 60 | 0;

        minutes = hours > 0 ? minutes % 60 : minutes;
        minutes = minutes < 10 ? '' + minutes : minutes;
        seconds = seconds % 60;
        seconds < 10 ? '' + seconds : seconds;

        return (time > 0 ? '+' : '') +
          (hours > 0 ? hours + 'h ' : '') +
          (minutes > 0 ? minutes + 'm ' : '') +
          (seconds + 's');
      },

      _calculateETA: function(time) {
        let currentDateTime = new Date();

        if (time < 0) {
            currentDateTime.setSeconds(currentDateTime.getSeconds() - time);
        }

        let hours = currentDateTime.getHours();
        let minutes = currentDateTime.getMinutes();

        // Determine AM/PM and convert hours to 12-hour format
        let period = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        // the hour '0' should be '12' in 12-hour time
        hours = hours ? hours : 12;

        // Formatting for display
        minutes = minutes < 10 ? '0' + minutes : minutes;

        return `${hours}:${minutes} ${period}`;
      }
    });