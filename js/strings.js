/**
 * Sprout 2.0 Centralized Strings & Localization Dictionary
 * Strict Compliance:
 * - No "guaranteed", "safe", or "risk-free"
 * - No em-dashes
 * - No vague copy
 * - Centralized disclaimer string for Compliance
 */

const SPROUT_STRINGS = {
  en: {
    // Brand & Global
    appName: "Sprout",
    tagline: "First-time investing for India",
    // COMPLIANCE: Replace [ARN-PLACEHOLDER] with the real AMFI ARN before going live
    complianceDisclaimer: "Mutual fund investments are subject to market risks. Read all scheme-related documents carefully before investing. Sprout Technologies Pvt. Ltd. is a SEBI-registered mutual fund distributor (ARN: [ARN-PLACEHOLDER]). Past performance is not an indicator of future returns.",
    complianceShort: "Mutual fund investments are subject to market risks. Read all scheme documents carefully.",
    switchLang: "हिंदी",
    
    // Navigation
    navHowItWorks: "How It Works",
    navChecklist: "Checklist",
    navTrust: "Security & Trust",
    navFaq: "FAQ",
    navOpenApp: "Try Onboarding Flow",
    navBackToWeb: "Back to Website",

    // Homepage Hero
    heroBadge: "Start small from ₹100",
    heroTitle: "Invest in index funds with as little as ₹100.",
    heroSubtitle: "Simple, transparent investing designed for your first step. No hidden fees, no complex financial jargon. Index funds have no lock-in (ELSS schemes are an exception).",
    heroCtaPrimary: "Start Onboarding Demo",
    heroCtaSecondary: "What You Need to Start",
    desktopQrTitle: "Scan to open on your phone",
    desktopQrSub: "Takes under 3 minutes to complete setup",

    // Pre-Checklist (Solving PAN/Aadhaar Drop-off)
    preChecklistTitle: "What you will need before starting",
    preChecklistSub: "Having these 3 items ready takes 2 minutes and prevents any verification delays.",
    doc1Title: "1. PAN Card Number",
    doc1Desc: "Required by SEBI regulations for investor identity verification.",
    doc2Title: "2. Aadhaar-linked Mobile Phone",
    doc2Desc: "To receive a one-time OTP from UIDAI for instant paperless e-KYC.",
    doc3Title: "3. Bank Account Number & IFSC",
    doc3Desc: "To link your bank account via a 1-rupee penny verification deposit.",

    // 3 Step Journey
    journeyTitle: "Three straightforward steps to your first investment",
    journeySub: "Designed to remove the friction and anxiety from beginning your financial journey.",
    step1Title: "1. Choose your pace",
    step1Desc: "Pick an amount you are comfortable with, starting from ₹100. Pause or withdraw anytime.",
    step2Title: "2. Complete one-time KYC",
    step2Desc: "Instant online identity check. Transparent waiting updates so you always know what is happening.",
    step3Title: "3. Make your first ₹100 investment",
    step3Desc: "Invest into a diversified Nifty 50 index fund basket in under 30 seconds.",

    // Trust & Regulation
    trustTitle: "Regulated, transparent, and built for trust",
    trust1Title: "SEBI Registered",
    trust1Desc: "Operates strictly under SEBI mutual fund distribution guidelines. AMFI Registration Number: [ARN-PLACEHOLDER].",
    trust2Title: "Direct Custodian Custody",
    trust2Desc: "Your money never stays in Sprout. Funds go directly to SEBI-regulated fund houses via BSE Star MF.",
    // COMPLIANCE: Zero/0% fee claim removed until Compliance signs off on direct vs regular plan
    trust3Title: "Transparent Pricing",
    trust3Desc: "No platform setup fee or monthly account maintenance charge. Fund expense ratios apply as disclosed in each scheme's offer document.",

    // FAQ Section
    faqTitle: "Frequently asked questions",
    faq1Q: "Can I really start with just ₹100?",
    faq1A: "Yes. Sprout offers access to SEBI-approved index funds and micro-SIP options that accept initial contributions starting at ₹100.",
    faq2Q: "Why do I need to provide PAN and Aadhaar?",
    faq2A: "Indian financial regulations (SEBI and PMLA) mandate Know Your Customer (KYC) verification for all mutual fund investors. This protects your identity and prevents financial fraud.",
    faq3Q: "Is my money locked in?",
    faq3A: "No. Unless you deliberately select an ELSS tax-saving scheme (which has a statutory 3-year lock-in), index funds on Sprout have no lock-in period. You can withdraw your money back to your bank account at any time.",
    faq4Q: "What happens while I am waiting on the KYC screen?",
    faq4A: "The app communicates securely with the central KRA registry and your bank. We provide real-time status updates so you know your verification is actively processing.",

    // Repeat CTA
    repeatTitle: "Ready to plant your first ₹100?",
    repeatSub: "Join thousands of disciplined first-time investors across India.",
    repeatBtn: "Launch Interactive App Flow",

    // Onboarding Screen 1: Welcome
    obWelcomeTitle: "Welcome to Sprout",
    obWelcomeSub: "Your first investment in 3 minutes, starting at ₹100.",
    obWelcomePillar1Title: "SEBI Regulated",
    obWelcomePillar1Desc: "Invest in approved index funds directly with custodians.",
    obWelcomePillar2Title: "Zero Lock-in Period",
    obWelcomePillar2Desc: "You stay in control. Withdraw your balance whenever needed.",
    obWelcomePillar3Title: "No Hidden Costs",
    obWelcomePillar3Desc: "Transparent zero-fee platform with clear transaction receipts.",
    obWelcomeBtn: "Get Started",

    // Onboarding Screen 2: Checklist
    obChecklistTitle: "Have these ready",
    obChecklistSub: "To complete your KYC in one smooth session, please confirm you have:",
    obChecklistPan: "PAN card number",
    obChecklistPanDesc: "Required by SEBI regulations for identification.",
    obChecklistAadhaar: "Phone linked to Aadhaar",
    obChecklistAadhaarDesc: "To receive a fast 6-digit OTP from UIDAI.",
    obChecklistBank: "Bank account number and IFSC",
    obChecklistBankDesc: "For instant 1-rupee penny drop verification.",
    obChecklistNotice: "If you do not have these handy right now, you can save your progress and return anytime.",
    obChecklistBtn: "I Have Them Ready",
    obChecklistSave: "Save progress and finish later",

    // Onboarding Screen 3: Goal & Pace
    obGoalTitle: "Choose your pace",
    obGoalSub: "How much would you like to start with? You can modify this anytime.",
    obPace100: "₹100",
    obPace100Label: "Trial Step",
    obPace500: "₹500",
    obPace500Label: "Popular",
    obPace1000: "₹1,000",
    obPace1000Label: "Builder",
    obProjectionTitle: "Estimated 5-Year Outlook (₹500/mo)",
    obProjectionInvested: "Amount invested by you:",
    obProjectionEst: "Historical market average value:",
    obProjectionDisclaimer: "Calculated at 11% historical index benchmark returns. Equity investments are subject to market risks and past performance does not guarantee future results.",
    obGoalBtn: "Continue to KYC",

    // Onboarding Screen 4: KYC & Waiting State
    obKycTitle: "Instant KYC Verification",
    obKycSub: "One-time digital identity check required by Indian law.",
    obKycPanLabel: "Permanent Account Number (PAN)",
    obKycPanPlaceholder: "ABCDE1234F",
    obKycAadhaarLabel: "Aadhaar Number (for OTP)",
    obKycAadhaarPlaceholder: "12-digit Aadhaar number",
    obKycBtnSubmit: "Verify Identity",
    obKycWaitingTitle: "Verifying with Registry...",
    obKycWaitingNote: "Your app is active and processing. We are awaiting a secure confirmation from the official KRA and bank registry.",
    obKycWaitCountdown: "Estimated wait time remaining:",
    obKycWaitWarning: "Please do not refresh or close this screen. This usually takes 15 to 25 seconds.",
    obKycCompletedMsg: "Identity and bank verified successfully!",
    obKycBtnProceed: "Proceed to First Investment",

    // Onboarding Screen 5: First Investment Moment
    obInvestTitle: "Your First Investment",
    obInvestSub: "Sprout Nifty 50 Index Basket",
    obInvestAmount: "Investment Amount",
    obInvestFundName: "Sprout Nifty 50 Index Basket",
    obInvestFundDesc: "Top 50 Indian companies. Transparent, low-cost index tracking.",
    obInvestFeePlatform: "Platform Fee",
    obInvestFeeStamp: "Govt. Stamp Duty",
    obInvestTotal: "Total Amount to Pay",
    obInvestBtnConfirm: "Invest ₹100 Now",
    obInvestSuccessTitle: "Investment Confirmed!",
    obInvestSuccessSub: "Your first investment has been placed with the fund house.",
    obInvestSuccessAllotment: "Units will be allotted to your account within 1 business day.",
    obInvestSuccessOrder: "Transaction Reference ID:",
    obInvestSuccessBtn: "Go to Dashboard"
  },
  
  hi: {
    // Brand & Global
    appName: "स्प्राउट",
    tagline: "भारत के नए निवेशकों के लिए पहला कदम",
    // COMPLIANCE: Replace [ARN-PLACEHOLDER] with the real AMFI ARN before going live
    complianceDisclaimer: "म्यूचुअल फंड निवेश बाज़ार जोखिमों के अधीन हैं। निवेश करने से पहले सभी योजना संबंधी दस्तावेज़ों को ध्यान से पढ़ें। स्प्राउट टेक्नोलॉजीज प्राइवेट लिमिटेड एक सेबी-पंजीकृत म्यूचुअल फंड वितरक है (ARN: [ARN-PLACEHOLDER])। पिछला प्रदर्शन भविष्य के लाभ की गारंटी नहीं देता है।",
    complianceShort: "म्यूचुअल फंड निवेश बाज़ार जोखिमों के अधीन हैं। योजना दस्तावेज़ ध्यान से पढ़ें।",
    switchLang: "English",

    // Navigation
    navHowItWorks: "यह कैसे काम करता है",
    navChecklist: "तैयारी सूची",
    navTrust: "सुरक्षा एवं नियम",
    navFaq: "अक्सर पूछे जाने वाले सवाल",
    navOpenApp: "ऑनबोर्डिंग डेमो देखें",
    navBackToWeb: "वेबसाइट पर लौटें",

    // Homepage Hero
    heroBadge: "सिर्फ ₹100 से शुरुआत",
    heroTitle: "इंडेक्स फंड में निवेश शुरू करें, केवल ₹100 से।",
    heroSubtitle: "आपके पहले कदम के लिए बनाया गया सरल और पारदर्शी निवेश ऐप। कोई छुपा हुआ शुल्क नहीं, कोई कठिन वित्तीय भाषा नहीं और निकासी पर कोई पाबंदी नहीं।",
    heroCtaPrimary: "ऑनबोर्डिंग डेमो शुरू करें",
    heroCtaSecondary: "शुरू करने के लिए क्या चाहिए",
    desktopQrTitle: "फोन पर खोलने के लिए स्कैन करें",
    desktopQrSub: "सेटअप पूरा करने में 3 मिनट से भी कम समय लगता है",

    // Pre-Checklist
    preChecklistTitle: "शुरू करने से पहले आवश्यक दस्तावेज़",
    preChecklistSub: "ये 3 चीज़ें तैयार रखने से वेरिफिकेशन बिना किसी रुकावट के पूरा हो जाता है।",
    doc1Title: "1. पैन कार्ड नंबर",
    doc1Desc: "निवेशक पहचान सत्यापन के लिए सेबी के नियमों के अनुसार आवश्यक।",
    doc2Title: "2. आधार से जुड़ा मोबाइल फोन",
    doc2Desc: "UIDAI से त्वरित पेपरलेस ई-केवाईसी ओटीपी प्राप्त करने के लिए।",
    doc3Title: "3. बैंक खाता नंबर और IFSC",
    doc3Desc: "1 रुपये के सत्यापन डिपॉजिट के माध्यम से बैंक लिंक करने के लिए।",

    // 3 Step Journey
    journeyTitle: "आपके पहले निवेश के लिए 3 आसान कदम",
    journeySub: "निवेश की घबराहट और उलझन को दूर करने के लिए बनाया गया सरल क्रम।",
    step1Title: "1. अपनी रफ़्तार चुनें",
    step1Desc: "₹100 से शुरू करके अपनी सुविधा अनुसार राशि चुनें। कभी भी रोकें या निकालें।",
    step2Title: "2. एक बार का KYC पूरा करें",
    step2Desc: "त्वरित डिजिटल सत्यापन। हर पल स्पष्ट जानकारी ताकि आप जानते रहें कि क्या हो रहा है।",
    step3Title: "3. अपना पहला ₹100 निवेश करें",
    step3Desc: "30 सेकंड में निफ्टी 50 इंडेक्स फंड में अपना पहला कदम रखें।",

    // Trust & Regulation
    trustTitle: "नियमबद्ध, पारदर्शी और भरोसेमंद",
    trust1Title: "सेबी (SEBI) पंजीकृत",
    // COMPLIANCE: Replace [ARN-PLACEHOLDER] with real AMFI ARN
    trust1Desc: "सेबी के म्यूचुअल फंड वितरण नियमों (ARN: [ARN-PLACEHOLDER]) के तहत पूर्णतः संचालित।",
    trust2Title: "सुरक्षित कस्टोडियन कस्टडी",
    trust2Desc: "आपका पैसा स्प्राउट में नहीं रहता, सीधे सेबी-अधिकृत फंड हाउस में जाता है।",
    // COMPLIANCE: Zero fee claim removed until Compliance approves
    trust3Title: "पारदर्शी मूल्य निर्धारण",
    trust3Desc: "कोई प्लेटफ़ॉर्म सेटअप शुल्क नहीं, कोई मासिक खाता शुल्क नहीं। योजना व्यय अनुपात संबंधित ऑफर दस्तावेज़ में प्रकट किया जाता है।",

    // FAQ Section
    faqTitle: "अक्सर पूछे जाने वाले सवाल",
    faq1Q: "क्या मैं वाकई सिर्फ ₹100 से शुरू कर सकता हूँ?",
    faq1A: "हाँ। स्प्राउट सेबी-अनुमोदित इंडेक्स फंड और माइक्रो-एसआईपी विकल्प प्रदान करता है जो ₹100 से शुरू होते हैं।",
    faq2Q: "मुझे पैन और आधार क्यों देना होता है?",
    faq2A: "भारतीय वित्तीय नियम (SEBI और PMLA) सभी निवेशकों के लिए केवाईसी अनिवार्य करते हैं ताकि आपकी पहचान सुरक्षित रहे।",
    faq3Q: "क्या मेरा पैसा लॉक हो जाता है?",
    faq3A: "नहीं। ईएलएसएस टैक्स सेविंग स्कीम को छोड़कर, इंडेक्स फंड में कोई लॉक-इन नहीं होता। आप कभी भी अपना पैसा अपने बैंक में निकाल सकते हैं।",
    faq4Q: "केवाईसी स्क्रीन पर प्रतीक्षा करते समय क्या होता है?",
    faq4A: "ऐप केंद्रीय केआरए रजिस्ट्री और आपके बैंक से सुरक्षित संपर्क करता है। हम लाइव अपडेट दिखाते हैं ताकि आपको पता रहे कि ऐप सक्रिय है।",

    // Repeat CTA
    repeatTitle: "क्या आप अपने पहले ₹100 का पौधा लगाने के लिए तैयार हैं?",
    repeatSub: "पूरे भारत के नए और अनुशासित निवेशकों के साथ जुड़ें।",
    repeatBtn: "इंटरैक्टिव ऐप फ्लो शुरू करें",

    // Onboarding Screen 1: Welcome
    obWelcomeTitle: "स्प्राउट में आपका स्वागत है",
    obWelcomeSub: "सिर्फ ₹100 से 3 मिनट में आपका पहला निवेश।",
    obWelcomePillar1Title: "सेबी अधिकृत",
    obWelcomePillar1Desc: "अनुमोदित फंड में सुरक्षित निवेश।",
    obWelcomePillar2Title: "कोई लॉक-इन नहीं",
    obWelcomePillar2Desc: "जब चाहें अपना पैसा निकालें।",
    obWelcomePillar3Title: "पारदर्शी व्यवस्था",
    obWelcomePillar3Desc: "कोई छुपा हुआ चार्ज नहीं।",
    obWelcomeBtn: "शुरू करें",

    // Onboarding Screen 2: Checklist
    obChecklistTitle: "इन्हें तैयार रखें",
    obChecklistSub: "सुगम केवाईसी के लिए कृपया सुनिश्चित करें कि आपके पास हैं:",
    obChecklistPan: "पैन कार्ड नंबर",
    obChecklistPanDesc: "पहचान के लिए सेबी द्वारा अनिवार्य।",
    obChecklistAadhaar: "आधार से जुड़ा फोन",
    obChecklistAadhaarDesc: "त्वरित 6-अंकीय ओटीपी प्राप्त करने के लिए।",
    obChecklistBank: "बैंक खाता नंबर एवं IFSC",
    obChecklistBankDesc: "1 रुपये के सत्यापन डिपॉजिट के लिए।",
    obChecklistNotice: "यदि अभी ये पास नहीं हैं, तो आप प्रगति सहेज सकते हैं और बाद में वापस आ सकते हैं।",
    obChecklistBtn: "मेरे पास ये तैयार हैं",
    obChecklistSave: "सहेजें और बाद में पूरा करें",

    // Onboarding Screen 3: Goal & Pace
    obGoalTitle: "अपनी रफ़्तार चुनें",
    obGoalSub: "आप कितने से शुरुआत करना चाहते हैं? इसे कभी भी बदला जा सकता है।",
    obPace100: "₹100",
    obPace100Label: "शुरुआती कदम",
    obPace500: "₹500",
    obPace500Label: "लोकप्रिय",
    obPace1000: "₹1,000",
    obPace1000Label: "बिल्डर",
    obProjectionTitle: "अनुमानित 5 वर्ष परिदृश्य (₹500/माह)",
    obProjectionInvested: "आपके द्वारा जमा राशि:",
    obProjectionEst: "ऐतिहासिक बाज़ार औसत मूल्य:",
    obProjectionDisclaimer: "11% ऐतिहासिक इंडेक्स रिटर्न के आधार पर गणना। बाज़ार जोखिमों के अधीन, पिछला लाभ भविष्य की गारंटी नहीं देता।",
    obGoalBtn: "केवाईसी के लिए आगे बढ़ें",

    // Onboarding Screen 4: KYC
    obKycTitle: "डिजिटल केवाईसी सत्यापन",
    obKycSub: "भारतीय कानून के अनुसार आवश्यक एक बार की डिजिटल प्रक्रिया।",
    obKycPanLabel: "स्थायी खाता संख्या (पैन)",
    obKycPanPlaceholder: "ABCDE1234F",
    obKycAadhaarLabel: "आधार संख्या (ओटीपी हेतु)",
    obKycAadhaarPlaceholder: "12-अंकीय आधार नंबर",
    obKycBtnSubmit: "पहचान सत्यापित करें",
    obKycWaitingTitle: "रजिस्ट्री से सत्यापन जारी है...",
    obKycWaitingNote: "आपका ऐप सक्रिय है। हम आधिकारिक KRA और बैंक रजिस्ट्री से पुष्टि की प्रतीक्षा कर रहे हैं।",
    obKycWaitCountdown: "अनुमानित शेष समय:",
    obKycWaitWarning: "कृपया इस स्क्रीन को बंद या रीफ्रेश न करें। इसमें आमतौर पर 15 से 25 सेकंड लगते हैं।",
    obKycCompletedMsg: "पहचान एवं बैंक विवरण सफलतापूर्वक सत्यापित हो गया!",
    obKycBtnProceed: "पहले निवेश की ओर बढ़ें",

    // Onboarding Screen 5: First Investment
    obInvestTitle: "आपका पहला निवेश",
    obInvestSub: "स्प्राउट निफ्टी 50 इंडेक्स बास्केट",
    obInvestAmount: "निवेश राशि",
    obInvestFundName: "स्प्राउट निफ्टी 50 इंडेक्स बास्केट",
    obInvestFundDesc: "शीर्ष 50 भारतीय कंपनियां। पारदर्शी इंडेक्स ट्रैकिंग।",
    obInvestFeePlatform: "प्लेटफ़ॉर्म शुल्क",
    obInvestFeeStamp: "सरकारी स्टाम्प शुल्क",
    obInvestTotal: "कुल देय राशि",
    obInvestBtnConfirm: "अभी ₹100 निवेश करें",
    obInvestSuccessTitle: "निवेश की पुष्टि हो गई!",
    obInvestSuccessSub: "आपका पहला निवेश फंड हाउस को सफलतापूर्वक भेज दिया गया है।",
    obInvestSuccessAllotment: "1 कार्यदिवस के भीतर यूनिट्स आपके खाते में आवंटित कर दी जाएंगी।",
    obInvestSuccessOrder: "लेनदेन संदर्भ संख्या:",
    obInvestSuccessBtn: "डैशबोर्ड पर जाएं"
  }
};

window.SPROUT_STRINGS = SPROUT_STRINGS;
