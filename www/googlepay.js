var exec = require('cordova/exec');

exports.isReadyToPay = function ( success, error) {
  exec(success, error, "GooglePay", "is_ready_to_pay", []);
};

exports.requestPayment = function ( totalPrice, currency,success, error) {
  exec(success, error, "GooglePay", "request_payment", [number]);
};
