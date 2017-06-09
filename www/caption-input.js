
//
// PhotoCaptionInputView.js
//
// Created by James Kong on  2017-04-18.
// Copyright 2017 James Kong. All rights reserved.

var cordova = require('cordova'),
    exec = require('cordova/exec');

var PhotoCaptionInputView = function() {
  // constructor
};

// Call this to register for push notifications and retreive a deviceToken
PhotoCaptionInputView.prototype.showCaptionInput = function(images, callback) {
  cordova.exec(callback, callback, "PhotoCaptionInputViewPlugin", "showCaptionInput", images ? [images] : []);
};



var photoCaptionInputView = new PhotoCaptionInputView();

module.exports = photoCaptionInputView;
