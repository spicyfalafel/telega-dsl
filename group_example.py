import asyncio
import logging
import os

from aiogram import Bot, Dispatcher
from aiogram.filters import CommandStart
from aiogram.fsm.state import State, StatesGroup
from aiogram.types import CallbackQuery, Message

from aiogram_dialog import (Dialog, DialogManager, StartMode, Window,
                            setup_dialogs)
from aiogram_dialog.widgets.kbd import Button, Next, SwitchTo
from aiogram_dialog.widgets.text import Const, Format


class Wizard(StatesGroup):
    start = State()
    group = State()
    end = State()


async def button1_clicked(
    callback: CallbackQuery, button: Button, manager: DialogManager
):
    manager.dialog_data["group"] = button.widget_id
    await manager.next()


async def cnl_edt_clicked(
    callback: CallbackQuery, button: Button, manager: DialogManager
):
    manager.dialog_data["group"] = "не задана"
    await manager.next()


dialog = Dialog(
    Window(
        Const("Добро пожаловать! Давайте начнем регистрацию"),
        Next(),
        SwitchTo(
            Const("Я не хочу регистрироваться"),
            id="cnl_edt",
            state=Wizard.end,
            on_click=cnl_edt_clicked,
        ),
        state=Wizard.start,
    ),
    Window(
        Const("Выберите вашу группу"),
        Button(Const("Group1"), id="Group1", on_click=button1_clicked),
        Button(Const("Group2"), id="Group2", on_click=button1_clicked),
        Button(Const("Group3"), id="Group3", on_click=button1_clicked),
        state=Wizard.group,
    ),
    Window(
        Format("Привет! группа {dialog_data[group]}"),
        state=Wizard.end,
    ),
)


async def start(message: Message, dialog_manager: DialogManager):
    await dialog_manager.start(Wizard.start, mode=StartMode.RESET_STACK)


async def main():
    # real main
    logging.basicConfig(level=logging.INFO)
    bot = Bot(token=os.getenv("BOT_TOKEN"))
    dp = Dispatcher()
    dp.include_router(dialog)
    dp.message.register(start, CommandStart())
    setup_dialogs(dp)

    await dp.start_polling(bot)
    config = RedisConfig(
            HOST=env.str("REDIS_HOST"),
            PORT=env.int("REDIS_PORT"),
            DB=env.int("REDIS_DB"),
        ),
    storage = RedisStorage.from_url(
        url=config.redis.dsn(),
        connection_kwargs={"decode_responses": True}
    )
    dp = Dispatcher(
        storage=storage,
    )


if __name__ == "__main__":
    asyncio.run(main())
