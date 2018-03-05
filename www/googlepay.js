var exec = require('cordova/exec');

var GooglePay = {
    isReadyToPay: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'GooglePay', 'isReadyToPay', []);
    },
    makePayment: function (data, successCallback, errorCallback) {
       exec(successCallback, errorCallback, 'GooglePay', 'makePayment', [data]);
    }
};

module.exports = GooglePay;