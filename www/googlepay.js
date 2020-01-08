var GooglePay = {
  isReadyToPay: function () {
    return new Promise(function (resolve, reject) {
      cordova.exec(resolve, reject, 'GooglePay', 'is_ready_to_pay', [])
    })
  },
  requestPayment: function (totalPrice, currency) {
    return new Promise(function (resolve, reject) {
      cordova.exec(resolve, reject, 'GooglePay', 'request_payment', [ totalPrice, currency ])
    })
  }
}

module.exports = GooglePay;
