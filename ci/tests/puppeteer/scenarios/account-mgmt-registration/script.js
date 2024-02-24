const puppeteer = require("puppeteer");
const cas = require("../../cas.js");

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await cas.gotoLogin(page);

    await cas.waitForTimeout(page, 2000);
    await cas.assertTextContent(page, "#accountSignUpLink", "Sign Up");
    await cas.submitForm(page, "#accountMgmtSignupForm");
    await cas.waitForTimeout(page, 1000);

    await cas.assertInnerText(page, "#content h2", "Account Registration");
    await cas.type(page,"#username", "casuser");
    await cas.type(page,"#firstName", "CAS");
    await cas.type(page,"#lastName", "Person");
    await cas.type(page,"#email", "cas@example.org");
    await cas.type(page,"#phone", "+1 347 745 1234");
    await cas.click(page, "#submit");
    await cas.waitForNavigation(page);
    await cas.assertInnerText(page, "#content h2", "Account Registration");
    await cas.assertInnerTextStartsWith(page, "#content p", "Account activation instructions are successfully sent");

    const link = await cas.extractFromEmailMessage(browser);
    await cas.log(`Activation link is ${link}`);
    await cas.goto(page, link);
    await cas.waitForTimeout(page, 1000);
    await cas.assertInnerText(page, "#content h2", "Account Registration");
    await cas.assertInnerTextStartsWith(page, "#content p", "Welcome back!");

    await typePassword(page, "EaP8R&iX$eK4nb8eAI", "EaP8R&iX$eK4nb8eAI");
    await cas.waitForTimeout(page, 1000);

    for (let i = 1; i <= 2; i++) {
        await cas.type(page, `#securityquestion${i}`, `Security question ${i}`);
        await cas.type(page, `#securityanswer${i}`, `Security answer ${i}`);
    }
    await cas.click(page, "#submit");
    await cas.waitForTimeout(page, 5000);
    await cas.assertInnerText(page, "#content h2", "Account Registration");
    await cas.assertInnerTextStartsWith(page, "#content p", "Thank you! Your account is now activated");
    await browser.close();
})();

async function typePassword(page, pswd, confirm) {
    await page.focus("#password");
    await cas.type(page,"#password", pswd);
    await page.$eval("#password", (e) => e.blur());
    await page.focus("#confirmedPassword");
    await cas.type(page,"#confirmedPassword", confirm);
    await page.$eval("#confirmedPassword", (e) => e.blur());
}
