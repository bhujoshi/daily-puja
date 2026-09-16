import Foundation

public enum PoojaStep: Int, CaseIterable, Sendable {
    case samagriSangrah = 1
    case deepaPrajwalan = 2
    case devSnan = 3
    case tilakPushparpan = 4
    case ghantiAarti = 5
    case shankhNaad = 6
    case bhogSamarpan = 7
    case aartiStuti = 8

    public var titleHi: String {
        switch self {
        case .samagriSangrah: return "सामग्री एकत्रीकरण"
        case .deepaPrajwalan: return "दीप प्रज्वलन एवं धूप"
        case .devSnan: return "देव स्नान एवं अभिषेक"
        case .tilakPushparpan: return "तिलक एवं पुष्पांजलि"
        case .ghantiAarti: return "घंटी वादन एवं आरती"
        case .shankhNaad: return "शंख नाद"
        case .bhogSamarpan: return "भोग एवं प्रसाद समर्पण"
        case .aartiStuti: return "आरती गायन"
        }
    }

    public var titleEn: String {
        switch self {
        case .samagriSangrah: return "Gather Items"
        case .deepaPrajwalan: return "Light Diya & Incense"
        case .devSnan: return "Bathing Deities (Abhishek)"
        case .tilakPushparpan: return "Tilak & Flower Offering"
        case .ghantiAarti: return "Bell Ringing & Aarti"
        case .shankhNaad: return "Blowing the Shankh"
        case .bhogSamarpan: return "Offering Prasad (Bhog)"
        case .aartiStuti: return "Singing Aarti"
        }
    }

    public var instructionHi: String {
        switch self {
        case .samagriSangrah: return "पूजा थाली में जल, ताजे पुष्प, चन्दन और नैवेद्य एकत्र करें।"
        case .deepaPrajwalan: return "दीपक की बाती पर ज्योति स्पर्श कराएं एवं अगरबत्ती प्रज्वलित करें।"
        case .devSnan: return "कलश से पावन जल भगवान गणेश व माता लक्ष्मी पर अर्पित करें और वस्त्र से पोंछें।"
        case .tilakPushparpan: return "प्रभु के मस्तक पर चन्दन व कुमकुम का तिलक लगाएं एवं ताजे पुष्प अर्पित करें।"
        case .ghantiAarti: return "पीतल की घंटी बजाते हुए आरती थाल को दक्षिणावर्त (गोल) घुमाएं।"
        case .shankhNaad: return "शंख को दबाकर रखें और पावन शंख ध्वनि से वातावरण को शुद्ध करें।"
        case .bhogSamarpan: return "भगवान को मोदक व खीर का भोग लगाएं और तीन बार आचमन जल अर्पित करें।"
        case .aartiStuti: return "श्रद्धा भाव से भगवान की आरती का गायन एवं श्रवण करें।"
        }
    }

    public var instructionEn: String {
        switch self {
        case .samagriSangrah: return "Gather fresh water, flowers, chandan, and offerings onto the thali."
        case .deepaPrajwalan: return "Light the ghee lamp wick and fragrant agarbatti."
        case .devSnan: return "Pour sacred water gently over deities and pat dry with silk cloth."
        case .tilakPushparpan: return "Apply sacred chandan & kumkum to foreheads and offer fresh blossoms."
        case .ghantiAarti: return "Ring the brass bell while rotating the Aarti Thali clockwise."
        case .shankhNaad: return "Hold the conch shell to resonate divine cosmic purity."
        case .bhogSamarpan: return "Offer Modak & Kheer with three circular achaman waves."
        case .aartiStuti: return "Immerse in divine surrender with the sacred Aarti recitation."
        }
    }

    public var mantraHi: String {
        switch self {
        case .samagriSangrah: return "ॐ पवित्रतायै नमः"
        case .deepaPrajwalan: return "शुभं करोति कल्याणम् आरोग्यम् धनसंपदा।"
        case .devSnan: return "ॐ अपवित्रः पवित्रो वा सर्वावस्थां गतोऽपि वा।"
        case .tilakPushparpan: return "ॐ श्री गणेशाय नमः | ॐ महालक्ष्म्यै नमः"
        case .ghantiAarti: return "कर्पूरगौरं करुणावतारं संसारसारम्..."
        case .shankhNaad: return "पाञ्चजन्य नमोऽस्तु ते"
        case .bhogSamarpan: return "ॐ प्राणाय स्वाहा, ॐ अपानाय स्वाहा..."
        case .aartiStuti: return "जय गणेश जय गणेश देवा / ॐ जय लक्ष्मी माता"
        }
    }

    public var mantraEn: String {
        switch self {
        case .samagriSangrah: return "Om Pavitratayai Namah"
        case .deepaPrajwalan: return "Shubham Karoti Kalyanam Aarogyam Dhanasampada."
        case .devSnan: return "Om Apavitrah Pavitro Va Sarvavastham Gato'pi Va."
        case .tilakPushparpan: return "Om Shri Ganeshaya Namah | Om Mahalaksmyai Namah"
        case .ghantiAarti: return "Karpura Gauram Karunavataram..."
        case .shankhNaad: return "Panchajanya Namostu Te"
        case .bhogSamarpan: return "Om Pranaya Swaha, Om Apanaya Swaha..."
        case .aartiStuti: return "Jai Ganesh Deva / Om Jai Lakshmi Mata"
        }
    }
}

public struct Deity: Identifiable, Sendable {
    public let id: String
    public let nameHi: String
    public let nameEn: String
    public var tilakApplied: BooleanLiteralType = false
    public var flowersOfferedCount: Int = 0

    public init(id: String, nameHi: String, nameEn: String, tilakApplied: Bool = false, flowersOfferedCount: Int = 0) {
        self.id = id
        self.nameHi = nameHi
        self.nameEn = nameEn
        self.tilakApplied = tilakApplied
        self.flowersOfferedCount = flowersOfferedCount
    }
}

public struct AgingState: Sendable {
    public let elapsedHours: Float
    public let dustLevel: Float          // 0.0 to 1.0
    public let flowerWitherFactor: Float // 0.0 to 1.0
    public let needsCleaning: Bool
    public let statusHi: String
    public let statusEn: String

    public init(elapsedHours: Float, dustLevel: Float, flowerWitherFactor: Float, needsCleaning: Bool, statusHi: String, statusEn: String) {
        self.elapsedHours = elapsedHours
        self.dustLevel = dustLevel
        self.flowerWitherFactor = flowerWitherFactor
        self.needsCleaning = needsCleaning
        self.statusHi = statusHi
        self.statusEn = statusEn
    }
}

public struct AartiLyric: Identifiable, Sendable {
    public var id: Int64 { timestampMs }
    public let timestampMs: Int64
    public let lineHi: String
    public let lineEn: String
    public let meaningEn: String

    public init(timestampMs: Int64, lineHi: String, lineEn: String, meaningEn: String) {
        self.timestampMs = timestampMs
        self.lineHi = lineHi
        self.lineEn = lineEn
        self.meaningEn = meaningEn
    }
}
