
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
PhotoCaptionInputView.prototype.showGallery = function(images, callback) {
  cordova.exec(callback, callback, "PhotoCaptionInputViewCordova", "showGallery", images ? [images] : []);
};

PhotoCaptionInputView.prototype.showBrowser = function(images, callback) {
  cordova.exec(callback, callback, "PhotoCaptionInputViewCordova", "showBrowser", images ? [images] : []);
};


var photoCaptionInputView = new PhotoCaptionInputView();

module.exports = photoCaptionInputView;
