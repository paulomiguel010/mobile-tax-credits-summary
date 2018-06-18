The Tax Credits Summary response
----
  Fetch the Tax Credits Summary object for a given nino.
  
* **URL**

  `/income/:nino/tax-credits/tax-credits-summary`

* **Method:**
  
  `GET`
  
*  **URL Params**

   **Required:**
 
   `nino=[Nino]`
   
   The nino given must be a valid nino. ([http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm](http://www.hmrc.gov.uk/manuals/nimmanual/nim39110.htm))

* **Success Responses:**

  * **Code:** 200 <br />
    **Note:** If the user is not excluded <br />
    **Content:**

```json
{
  "paymentSummary": {
    "workingTaxCredit": {
      "paymentSeq": [
        {
          "amount": 55,
          "paymentDate": 1509008158781,
          "oneOffPayment": false,
          "earlyPayment": false
        },
        {
          "amount": 55,
          "paymentDate": 1511690158781,
          "oneOffPayment": false,
          "holidayType": "bankHoliday",
          "earlyPayment": true,
          "explanatoryText" : "Your payment is early because of UK bank holidays."
        },
        {
          "amount": 55,
          "paymentDate": 1514282158781,
          "oneOffPayment": true,
          "earlyPayment": false,
          "explanatoryText" : "This is because of a recent change and is to help you get the right amount of tax credits."
        }
      ],
      "paymentFrequency": "weekly",
      "previousPaymentSeq": [
        {
          "amount": 33,
          "paymentDate": 1503737758781,
          "oneOffPayment": false,
          "earlyPayment": false
        },
        {
          "amount": 43,
          "paymentDate": 1501059358781,
          "oneOffPayment": false,
          "holidayType": "bankHoliday",
          "earlyPayment": true,
          "explanatoryText" : "Your payment was early because of UK bank holidays."
        },
        {
          "amount": 53,
          "paymentDate": 1498467358781,
          "oneOffPayment": true,
          "earlyPayment": false,
          "explanatoryText" : "This was because of a recent change and was to help you get the right amount of tax credits."
        }
      ]
    },
    "childTaxCredit": {
      "paymentSeq": [
        {
          "amount": 55,
          "paymentDate": 1509008158781,
          "oneOffPayment": false,
          "earlyPayment": false
        },
        {
          "amount": 55,
          "paymentDate": 1511690158781,
          "oneOffPayment": false,
          "holidayType": "bankHoliday",
          "earlyPayment": true,
          "explanatoryText" : "Your payment is early because of UK bank holidays."
        },
        {
          "amount": 55,
          "paymentDate": 1514282158781,
          "oneOffPayment": true,
          "earlyPayment": false,
          "explanatoryText" : "This is because of a recent change and is to help you get the right amount of tax credits."
        }
      ],
      "paymentFrequency": "weekly"
    },
    "paymentEnabled": true,
    "totalsByDate": [
      {
        "amount": 110,
        "paymentDate": 1509008158781
      },
      {
        "amount": 110,
        "paymentDate": 1511690158781
      },
      {
        "amount": 110,
        "paymentDate": 1514282158781
      }
    ],
    "previousTotalsByDate": [
      {
        "amount": 53,
        "paymentDate": 1498467358781
      },
      {
        "amount": 43,
        "paymentDate": 1501059358781
      },
      {
        "amount": 33,
        "paymentDate": 1503737758781
      }
    ]
  },
  "claimants": {
    "personalDetails": {
      "forename": "firstname",
      "surname": "surname"
    },
    "partnerDetails": {
      "forename": "Frederick",
      "otherForenames": "Tarquin",
      "surname": "Hunter-Smith"
    },
    "children": [
      {
        "forename": "Sarah",
        "surname": "Smith"
      },
      {
        "forename": "Joseph",
        "surname": "Smith"
      },
      {
        "forename": "Mary",
        "surname": "Smith"
      }
    ]
  }
}
```

  * **Code:** 200 <br />
    **Note:** If the user is excluded <br />
    **Content:**
    
```json
{
   "excluded": true
}
```

  * **Code:** 200 <br />
    **Note:** If the user is a non tax credits user <br />
    **Content:**
    
```json
{
   "excluded": false
}
```
 
* **Error Responses:**

  * **Code:** 401 UNAUTHORIZED <br/>
    **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

  * **Code:** 401 FORBIDDEN <br/>
    **Content:** `{"code":"FORBIDDEN","message":Authenticated user is not authorised for this resource"}`

  * **Code:** 404 NOTFOUND <br/>
    **Content:** `{ "code" : "MATCHING_RESOURCE_NOT_FOUND", "message" : "A resource with the name in the request can not be found in the API" }`

  OR when a user does not exist or server failure

  * **Code:** 500 INTERNAL_SERVER_ERROR <br/>



