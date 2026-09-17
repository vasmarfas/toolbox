package com.vasmarfas.card.tools.security

object WordLists {
    val english: List<String> = (
        "able acid acorn actor adapt adult after agent agree ahead alarm album alert alien alive alley alloy alone " +
            "alpha amber amble amend amber angel anger angle ankle apple apron arbor arena argue arise armor aroma " +
            "arrow asset atlas attic audio audit avert awake award aware badge bagel baker balmy banjo barge basil " +
            "basin batch beach beacon beam bean beard beast begin being bench berry birch birth bison black blade " +
            "blank blaze blend bliss block bloom blues bluff blunt board boast bonus boost booth borax bough bound " +
            "brace braid brain brake brand brass brave bread break breed brick bride brief bring brisk broad broil " +
            "brook broom brown brush buddy bugle build bunch bunny burst cabin cable cacao cadet cameo canal candy " +
            "canoe canon cargo carol carve catch cause cedar chain chair chalk charm chart chase cheek cheer chess " +
            "chest chief child chill chime choir chord chose churn cider cigar civic civil claim clamp clash clasp " +
            "clean clear clerk cliff climb cling cloak clock close cloud clove clown coach coast cobra cocoa colon " +
            "color comet comic coral corgi couch cough count court cover crack craft crane crash crate crawl craze " +
            "cream creek creep crest cried crisp cross crowd crown crumb crush crust cubic curve cycle daily dairy " +
            "dance dandy datum dealt debit debut decal decay decoy defer deity delay delta dense depot depth derby " +
            "detox devil diary digit diner dingo ditch diver dodge donor doubt dough dozen draft drain drake drama " +
            "drank dream dress dried drift drill drink drive drone drove drown dwell eagle early earth easel eaten " +
            "ebony eight elbow elder elect elite elope ember empty enact enemy enjoy enter entry equal equip erase " +
            "error essay ether ethic evade event every evict exact exalt excel exert exile exist extra fable fancy " +
            "fault favor feast fence ferry fetch fever fiber field fiery fifty fight final finch first flair flame " +
            "flank flash fleet flesh flick fling flint float flock flood floor flour fluid flute foamy focal focus " +
            "foggy force forge forte forth forty forum found frame fraud fresh fried frost fruit fudge fully funny " +
            "gauge gecko genre ghost giant given giver glade glare glass gleam globe gloom glory glove glyph gnome " +
            "going grace grade grain grand grant grape graph grasp grass grave gravy graze great green greet grief " +
            "grill grind groan groom group grove guard guess guest guide guild guilt habit hardy harsh haste hatch " +
            "haven hazel heart heavy hedge hefty hello hence herbs heron hinge hippo hobby holly honey honor horde " +
            "horse hotel hound house hover human humid humor hurry ideal image imply index inlet inner input ivory " +
            "jelly jewel joint jolly judge juice jumbo juror kayak kebab knack kneel knife knock known koala label " +
            "labor lager lance lapse large larva laser latch later laugh layer leaky learn lease leash least leave " +
            "ledge lemon level lever light lilac limit linen liner lingo liver llama lobby local lodge lofty logic " +
            "loose lotus lower loyal lucid lucky lunar lunch lyric magic major maker mango manor maple march marsh " +
            "match maybe mayor meant medal media melon mercy merge merit metal meter midst might mimic miner minor " +
            "minus mirth mixer model moist molar money month moral motor mound mount mourn mouse mouth mover movie " +
            "mural music naval nerve never newly niche night noble noise north notch novel nurse oasis occur ocean " +
            "offer often olive omega onion opera orbit order organ otter ought ounce outer owner oxide ozone paint " +
            "panel paper parka party pasta patch patio pause peach pearl pedal penny perch peril petal phase phone " +
            "photo piano piece pilot pinch pitch pivot pixel pizza place plain plane plank plant plate plaza plead " +
            "pluck plumb plume plush poems point polar porch poser pouch pound power prank press price pride prime " +
            "print prior prism prize probe prone proof proud prove prune pulse punch pupil puppy purse quail quake " +
            "quart queen query quest queue quick quiet quilt quirk quota quote radar radio raise rally ranch range " +
            "rapid ratio raven reach react ready realm rebel refer reign relax relay remit renew repay reply rerun " +
            "reset resin retro rider ridge rifle right rigid rinse risky rival river roast robin robot rocky rogue " +
            "roman rotor rouge rough round route royal rugby ruler rumor rural saber saint salad salon salsa salty " +
            "sandy sauce saute savor scale scarf scene scent scoop scope score scout scrap screw scrub sedan seize " +
            "sense serve seven shade shaft shake shale shall shape share shark sharp shawl shear sheep sheer shelf " +
            "shell shift shine shiny shire shirt shock shore short shout shove shown shrub siege sight sigma silky " +
            "silly since siren sixth skate skier skill skirt slate sleek sleep slice slide slope small smart smash " +
            "smile smoke snack snail snake sneak snowy solar solid solve sonic sorry sound south space spade spare " +
            "spark speak spear speed spell spend spent spice spike spine spiral splash split spoke spoon sport spout " +
            "spray spree squad squid stack staff stage stain stair stake stale stalk stamp stand stare stark start " +
            "state steak steam steel steep stern stick stiff still sting stock stoic stole stone stood stool store " +
            "storm stout stove strap straw strip study stuff stunt style sugar suite sunny super surge swamp swarm " +
            "swear sweat sweep sweet swift swing swirl sword table tacit taffy talon tango tanker tapir tarot taste " +
            "teach tease tempo tenor tense tenth thank theme there thick thigh thing think third thorn those three " +
            "throw thumb tidal tiger tight timer timid title toast today token tonic tooth topaz topic torch total " +
            "touch tough towel tower trace track trade trail train trait tramp trash tread treat trend trial tribe " +
            "trick tried tromp troop trout truce truck truly trunk trust truth tulip tumor tunic turbo tutor twice " +
            "twist ultra uncle under union unite unity until upper upset urban usage usher usual vague valid value " +
            "valve vapor vault venue verse video vigil villa vinyl viola viper virus visit vital vivid vocal vodka " +
            "vogue voice vouch vowel wager wagon waist waltz waste watch water waver weary weave wedge weigh weird " +
            "whale wheat wheel where which while whirl white whole whose widen widow width wield wince windy wiser " +
            "witty wolves woman world worry worth wound woven wrist write wrong yacht yeast yield young youth zebra " +
            "zesty zonal"
        ).split(' ').filter { it.isNotEmpty() }.distinct()

    val russian: List<String> = (
        "автор агент адрес азбука акция аллея алмаз альбом ангел апрель арбуз армия артист архив аспект астра атака " +
            "атлас аукцион бабочка багаж базар баклан балкон банан баржа барон баскет батон башня бегун беда бедро " +
            "берег берёза беседа билет бисер благо бланк блеск ближе блокнот блюдо бобёр богач бодрый боец болото " +
            "борода борщ ботва бочка боязнь бравый бревно бригада бронза брошь брусок бублик будни буква булка " +
            "бумага бунт буран бурый бутон буфет бухта былина бытие бюджет вагон важный валенок валюта ванна варенье " +
            "василёк ватага ведро вежливый вектор велосипед вена вера верба верёвка версия весло весна ветер вечер " +
            "вешалка взгляд взлёт видео вилка вино висок витраж вихрь вишня вклад власть влага внимание вода водопад " +
            "воздух возраст война вокзал волна волос вопрос ворота восток впадина врата время вспышка вторник вулкан " +
            "вход выбор вывод выдра выход вышка вязание гавань газета галета галка гамак гараж гарнир гвоздь гений " +
            "герб герой гитара глагол глаз глина глубина гнездо голос голубь гонка гора гордость город горох гость " +
            "грамота гранат граница график гребень греча гриб гроза грудь группа груша гряда губка гудок гусар густой " +
            "дама дача двор девиз дежурный декабрь дело дерево десант десять деталь дети дефис дневник добро довод " +
            "дождь доклад долина доля дом домино дорога доска дочь дракон дрова друг дуб дуга дудка дума дуэт дыня " +
            "дыхание дятел ежевика езда ёлка енот ефрейтор жажда жалоба жара жемчуг желание железо жертва жетон жилет " +
            "жираф житель журавль жюри забава забор завод загадка задача закат закон замок запад запас заряд заря " +
            "звезда звонок здание зебра зелень земля зеркало зерно зима злак знак знание зодчий золото зонт зубр " +
            "игла игра идея избушка известь изгиб изюм икра иллюзия имение импульс инжир иней иностранец институт " +
            "инструмент интерес искра искусство исток итог июль июнь кабина кабель кактус календарь камень камыш " +
            "канат капель капитан карандаш карман карта картина каскад касса катер качели каштан квартал кедр кисель " +
            "кисть кит клавиша кладовая класс клевер клён клетка клиент клубок ключ книга кнопка ковёр когорта кожа " +
            "колесо колодец колокол колос кольцо комар комета команда компас конверт конус копия корабль корень корзина " +
            "коридор корона космос кость костёр котёл кофе кошка крабы край краска кратер кредит крепость кресло " +
            "кристалл крокодил кролик кровля круг крыло крыша кузнец кукла культура купол курган курс кухня лабиринт " +
            "лавина лагерь ладонь лампа ландыш лапша ларец ласточка лебедь легенда лёд лекция лента лепесток лес " +
            "лестница лето лимон линза линия липа лиса листва литр лицей личность лодка ложка локоть лопата лоскут " +
            "лось лоток лужайка луна лупа луч лыжи любовь люстра лягушка магазин магнит маковка малина мальчик мангал " +
            "манжета маршрут маска масло мастер мачта маяк мебель медведь медаль мёд мельница месяц металл метель метод " +
            "метро механизм мечта мешок миндаль минута мираж мишень мнение могила модель мозаика молния молоко момент " +
            "монета море морковь мороз мостик мотор мрамор музей музыка мысль мышь мята набат наволочка награда надежда " +
            "накидка налим напиток народ наряд наука начало небо невод неделя нектар нерв нива нитка ножницы номер " +
            "норка носорог нота ноябрь нужда обед облако обложка образ обувь община овраг овца огонь огород огурец " +
            "одеяло озеро океан окно окраина октябрь олень олива омлет опора оптика орбита орех оркестр оружие осадок " +
            "осень осина остров отвага ответ отдых отель отрывок отряд очаг очки ошибка павлин палата палец палитра " +
            "пальма память панель папка парад парус паста патруль пауза паук пекарь пеликан пенал пепел перевал перец " +
            "перила перо песня песок петля печать пирог письмо пища планета пластина платок плечо плита площадь плот " +
            "победа повар повод погода подарок подкова поезд пожар покой поле полка полночь полоса помощь понедельник " +
            "порог портрет посёлок посуда поток поход почва почта поэма правда праздник практика предмет прибой привет " +
            "призма пример природа причал проба провод программа продукт проект простор протока профиль прохлада процесс " +
            "прыжок прялка пряник птица пудра пустыня путник пуговица пшеница пятница радость радуга развилка разговор " +
            "район ракета рамка ранец распорядок растение рассвет расчёт ребус регион редис режим резец река реклама " +
            "рельеф ремень рецепт речка решение ржавчина рисунок ритм роба ровесник родник рожок роза розетка роса рост " +
            "роща рубеж рубин рукав руль ручей рыба рынок рысь рюкзак рябина ряска сабля садовник салат салют самолёт " +
            "сапфир сарай сахар свеча свобода связка север секунда сельдь семья сентябрь сервиз сердце серебро сестра " +
            "сеть сигнал сила синица система скала скамья сказка скала склон скобка скорость скрипка слава слеза слово " +
            "слон случай смелость смена смола снег сноп собака событие совет созвездие сокол солнце соль сосна сотня " +
            "спектр спица спорт справка среда ставня стакан сталь станок старт статья стебель стекло стена степь стиль " +
            "столица сторона страница стрела строка струна студень стужа ступень стул судно сумка суп сустав суббота " +
            "сухарь схема сцена счёт съезд сырьё сюжет табак таблица тайга тайна такси талант тарелка творог театр " +
            "тележка телефон тема тень теорема тепло терем термин тесто тетрадь техника течение тигр типаж тираж тишина " +
            "ткань товар ток тополь топор торжество торт точка трава трактор трамвай тревога тропа труба трюм туман " +
            "тундра туннель туча тыква тюлень тяга уборка увлечение угол удача удочка ужин узел указ уклон украшение " +
            "улей улица улыбка умение уровень урожай усадьба услуга устав утёс утро ухо участок учебник ученик учитель " +
            "ущелье фабрика фазан факел факт фарватер фартук фасад фаза февраль ферма фигура фильм финал фиалка флаг " +
            "флейта флот фонарь фонтан форель форма фото фраза фрегат фрукт фундамент футбол фуражка характер хвоя " +
            "хижина хлеб хмель холм хозяин холст хоровод храм хрусталь худой цапля царство цветок цепочка цикл цилиндр " +
            "цирк цитата чайка чайник часовня часть чаща чемодан череп черешня чертёж чеснок четверг число чтение чугун " +
            "чудо чулок шалаш шампунь шапка шарф шахта шашки швея шелест шёлк шеренга шестерня шинель ширина шишка " +
            "школа шлем шляпа шнурок шоколад шорох шоссе штиль шторм штурман шуба шум шутка щавель щенок щётка щит " +
            "эвкалипт экипаж экран экспорт электрон эмблема энергия эпоха этаж эфир эхо юбилей юнга юность юрта яблоко " +
            "ягода якорь яма январь ярлык ярмарка ясень яхта ячмень ящерица ящик"
        ).split(' ').filter { it.isNotEmpty() }.distinct()
}
