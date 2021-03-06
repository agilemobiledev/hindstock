/*
 * Copyright 2012 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.kevinsawicki.hindstock;

import android.os.AsyncTask;

import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.github.kevinsawicki.stocks.DateUtils;
import com.github.kevinsawicki.stocks.StockQuoteRequest;

import java.io.IOException;
import java.util.Calendar;

/**
 * Request class to compute the gain/loss
 */
public abstract class GainLossRequest extends AsyncTask<Void, Integer, Quote> {

  private final String symbol;

  private final float shares;

  private final float dollars;

  private final Calendar buyDate;

  private final Calendar sellDate;

  private IOException exception;

  /**
   * Create request
   *
   * @param symbol
   * @param shares
   * @param dollars
   * @param buyDate
   * @param sellDate
   */
  public GainLossRequest(final String symbol, final float shares,
      final float dollars, final Calendar buyDate, final Calendar sellDate) {
    this.symbol = symbol;
    this.shares = shares;
    this.dollars = dollars;
    this.buyDate = buyDate;
    this.sellDate = sellDate;
  }

  /**
   * @return exception
   */
  public IOException getException() {
    return exception;
  }

  @Override
  protected Quote doInBackground(Void... params) {
    exception = null;
    try {
      float buyPrice = getBuyPrice();
      float sellPrice = getSellPrice();
      float totalShares;
      if (dollars > 0)
        totalShares = dollars / buyPrice;
      else
        totalShares = shares;
      return new Quote(buyPrice, sellPrice, totalShares);
    } catch (IOException e) {
      exception = e;
      return null;
    }
  }

  private float getBuyPrice() throws IOException {
    StockQuoteRequest buyRequest = new StockQuoteRequest();
    buyRequest.setStartDate(DateUtils.addDays(-7, buyDate)).setEndDate(buyDate)
        .setSymbol(symbol);

    try {
      if (!buyRequest.next())
        throw new InvalidBuyDateException();
    } catch (HttpRequestException e) {
      throw e.getCause();
    }

    float price = buyRequest.getOpen();
    if (price <= 0.0F)
      price = buyRequest.getClose();
    return price;

  }

  private float getSellPrice() throws IOException {
    StockQuoteRequest sellRequest = new StockQuoteRequest();
    sellRequest.setStartDate(DateUtils.addDays(-7, sellDate))
        .setEndDate(sellDate).setSymbol(symbol);

    try {
      if (!sellRequest.next())
        throw new InvalidSellDateException();
    } catch (HttpRequestException e) {
      throw e.getCause();
    }

    float price = sellRequest.getClose();
    if (price <= 0.0F)
      price = sellRequest.getOpen();
    return price;
  }

  @Override
  protected void onPostExecute(final Quote result) {
    if (result != null)
      onSuccess(result);
    else
      onFailure(exception);
  }

  /**
   * Called after request completes and the net amount has been computed
   *
   * @param quote
   */
  protected abstract void onSuccess(Quote quote);

  /**
   * Called when request fails providing the cause of the failure
   *
   * @param cause
   */
  protected abstract void onFailure(IOException cause);
}
