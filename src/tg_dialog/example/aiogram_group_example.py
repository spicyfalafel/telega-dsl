import asyncio
import logging
import sys
from os import getenv
from typing import Any, Dict

from aiogram import Bot, Dispatcher, F, Router, html
from aiogram.enums import ParseMode
from aiogram.filters import Command, CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from aiogram.types import (
    KeyboardButton,
    Message,
    ReplyKeyboardMarkup,
    ReplyKeyboardRemove,
)

TOKEN = getenv("BOT_TOKEN")
form_router = Router()

class MyStates(StatesGroup):
    welcome = State()
    end = State()

@form_router.message(CommandStart())
async def command_start(message: Message, state: FSMContext) -> None:
    await state.set_state(MyStates.group)
    await message.answer(
        "Добро пожаловать! Давайте начнем регистрацию.",
        reply_markup=ReplyKeyboardMarkup(
            keyboard=[
                [
                    KeyboardButton(text="Next"),
                    KeyboardButton(text="Я не хочу регистрироваться"),
                ]
            ]
        ),
    )

@form_router.message(MyStates.welcome, F.text.casefold() == "Next")
async def chosen_next(message: Message, state: FSMContext) -> None:
    await state.set_state(MyStates.end)
    await message.answer(
        "Выберите вашу группу:",
        reply_markup=ReplyKeyboardMarkup(
            keyboard=[
                [
                    KeyboardButton(text="Group1"),
                    KeyboardButton(text="Group2"),
                    KeyboardButton(text="Group3"),
                ]
            ]
        ),
    )

@form_router.message(MyStates.end)
@form_router.message(MyStates.welcome, F.text.casefold() == "Я не хочу регистрироваться")
async def end_step(message: Message, state: FSMContext) -> None:
    text = "Привет! Группа"
    ans = message.text
    if ans != "Я не хочу регистрироваться":
        await state.set_data(group=ans)
        text += ans
    else:
        text += "не задана"
    await message.answer(text)

async def main():
    bot = Bot(token=TOKEN, parse_mode=ParseMode.HTML)
    dp = Dispatcher()
    dp.include_router(form_router)
    await dp.start_polling(bot)

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, stream=sys.stdout)
    asyncio.run(main())
